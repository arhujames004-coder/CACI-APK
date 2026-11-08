import os
import io
import csv
import json
from decimal import Decimal
from datetime import datetime, date, timedelta
from flask import (
    Flask, request, Response, render_template, redirect,
    url_for, session, flash, jsonify
)

from tithe_storehouse.config import Config, calculate_charges
from tithe_storehouse.database import (
    get_db, init_db, normalize_phone, get_or_create_member
)
from tithe_storehouse.ussd_handler import handle_ussd_request
from tithe_storehouse.paystack_client import verify_paystack_signature
from tithe_storehouse.sms_service import send_receipt_sms, RECENT_SENT_SMS
from tithe_storehouse.audit import log_audit, get_audit_logs
from tithe_storehouse.rate_limiter import ussd_limiter, webhook_limiter, admin_limiter
from tithe_storehouse.auth import verify_admin_credentials, login_required

def create_app(test_config=None):
    app = Flask(__name__, template_folder="templates")
    app.config["SECRET_KEY"] = Config.SECRET_KEY

    # Initialize database on startup
    with app.app_context():
        init_db(seed=True)

    @app.context_processor
    def inject_global_config():
        return {"config": Config}

    # =========================================================================
    # 1. AFRICA'S TALKING USSD ENDPOINT
    # =========================================================================
    @app.route("/ussd", methods=["POST"])
    def ussd_callback():
        client_ip = request.remote_addr or "127.0.0.1"
        session_id = request.values.get("sessionId", "")
        service_code = request.values.get("serviceCode", Config.CHURCH_SHORTCODE)
        phone_number = request.values.get("phoneNumber", "")
        text = request.values.get("text", "")

        # Rate Limiting
        rate_key = phone_number or client_ip
        if not ussd_limiter.is_allowed(rate_key):
            log_audit(
                actor="ussd",
                action="USSD_RATE_LIMITED",
                entity_type="ussd_session",
                entity_id=session_id,
                details={"phone": phone_number, "ip": client_ip},
                ip_address=client_ip
            )
            return Response("END Too many requests. Please wait a moment and try again.", mimetype="text/plain")

        try:
            response_text = handle_ussd_request(
                session_id=session_id,
                service_code=service_code,
                phone_number=phone_number,
                text=text
            )
        except Exception as e:
            log_audit(
                actor="ussd",
                action="USSD_ERROR",
                entity_type="ussd_session",
                entity_id=session_id,
                details={"error": str(e), "phone": phone_number, "text": text},
                ip_address=client_ip
            )
            response_text = "END An unexpected error occurred. Please try again later."

        return Response(response_text, mimetype="text/plain")

    # =========================================================================
    # 2. PAYSTACK WEBHOOK ENDPOINT
    # =========================================================================
    @app.route("/webhook/paystack", methods=["POST"])
    def paystack_webhook():
        client_ip = request.remote_addr or "127.0.0.1"
        if not webhook_limiter.is_allowed(client_ip):
            return jsonify({"status": "error", "message": "Rate limited"}), 429

        raw_body = request.get_data()
        signature = request.headers.get("X-Paystack-Signature", "")

        # Verify HMAC-SHA512 Signature
        if not verify_paystack_signature(raw_body, signature):
            log_audit(
                actor="paystack_webhook",
                action="WEBHOOK_SIGNATURE_FAILED",
                entity_type="security",
                entity_id=signature,
                details={"ip": client_ip},
                ip_address=client_ip
            )
            return jsonify({"status": "error", "message": "Invalid signature"}), 400

        try:
            event_data = json.loads(raw_body.decode("utf-8"))
        except Exception as e:
            return jsonify({"status": "error", "message": "Malformed JSON"}), 400

        event = event_data.get("event")
        data = event_data.get("data", {})

        if event == "charge.success":
            reference = data.get("reference")
            paid_amount_pesewas = data.get("amount", 0)
            paid_amount_ghs = float(paid_amount_pesewas) / 100.0

            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT t.*, m.name as member_name, f.name as fund_name
                    FROM transactions t
                    LEFT JOIN members m ON t.member_id = m.id
                    LEFT JOIN funds f ON t.fund_code = f.code
                    WHERE t.reference = ?
                """, (reference,))
                tx = cursor.fetchone()

                if tx:
                    # Update status to success
                    cursor.execute("""
                        UPDATE transactions
                        SET status = 'success', paid_at = CURRENT_TIMESTAMP, sms_receipt_sent = 1
                        WHERE reference = ?
                    """, (reference,))

                    member_name = tx["member_name"] or ""
                    fund_name = tx["fund_name"] or tx["fund_code"].capitalize()
                    base_amount = tx["amount"]

                    # Dispatch SMS receipt
                    send_receipt_sms(
                        to_phone=tx["phone_number"],
                        member_name=member_name,
                        fund_name=fund_name,
                        base_amount=base_amount,
                        reference=reference
                    )

                    log_audit(
                        actor="paystack_webhook",
                        action="PAYMENT_CONFIRMED",
                        entity_type="transaction",
                        entity_id=reference,
                        details={
                            "amount_received": paid_amount_ghs,
                            "base_amount": base_amount,
                            "fee": tx["fee"],
                            "phone": tx["phone_number"]
                        },
                        ip_address=client_ip
                    )
                else:
                    # Direct payment or webhook received for external reference
                    log_audit(
                        actor="paystack_webhook",
                        action="UNMATCHED_PAYMENT_REF",
                        entity_type="transaction",
                        entity_id=reference,
                        details={"amount": paid_amount_ghs},
                        ip_address=client_ip
                    )

        return jsonify({"status": "ok"}), 200

    # =========================================================================
    # 3. LOCAL SIMULATED PAYMENT CHECKOUT (FOR TESTING WITHOUT LIVE KEYS)
    # =========================================================================
    @app.route("/pay/mock/<reference>", methods=["GET", "POST"])
    def mock_checkout(reference):
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT t.*, f.name as fund_name
                FROM transactions t
                LEFT JOIN funds f ON t.fund_code = f.code
                WHERE t.reference = ?
            """, (reference,))
            tx = cursor.fetchone()

        if not tx:
            return "Transaction reference not found", 404

        if request.method == "POST":
            # Simulate Paystack Webhook payload
            mock_payload = {
                "event": "charge.success",
                "data": {
                    "reference": reference,
                    "amount": int(round(tx["total_amount"] * 100)),
                    "status": "success",
                    "currency": tx["currency"]
                }
            }
            # Internal call to mark paid
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    UPDATE transactions
                    SET status = 'success', paid_at = CURRENT_TIMESTAMP, sms_receipt_sent = 1
                    WHERE reference = ?
                """, (reference,))

                send_receipt_sms(
                    to_phone=tx["phone_number"],
                    member_name="",
                    fund_name=tx["fund_name"] or tx["fund_code"].capitalize(),
                    base_amount=tx["amount"],
                    reference=reference
                )

                log_audit(
                    actor="simulator_donor",
                    action="PAYMENT_SIMULATED_SUCCESS",
                    entity_type="transaction",
                    entity_id=reference,
                    details={"total": tx["total_amount"], "phone": tx["phone_number"]}
                )

            flash(f"Payment successful! Receipt SMS sent to {tx['phone_number']}.", "success")
            return redirect(url_for("admin_dashboard"))

        return f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Paystack MoMo Checkout Simulation</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
        </head>
        <body class="bg-light p-3">
            <div class="card max-w-sm mx-auto shadow-sm p-4 mt-5" style="max-width: 420px;">
                <h5 class="fw-bold mb-1">{Config.CHURCH_NAME}</h5>
                <p class="text-muted small">Paystack Mobile Money & Card Gateway</p>
                <hr>
                <div class="mb-2"><strong>Giving Purpose:</strong> {tx['fund_name'] or tx['fund_code'].capitalize()}</div>
                <div class="mb-2"><strong>Base Gift:</strong> {tx['currency']} {tx['amount']:.2f}</div>
                <div class="mb-2 text-warning"><strong>Processing Fee:</strong> +{tx['currency']} {tx['fee']:.2f}</div>
                <div class="h4 fw-bold text-success mb-3">Total: {tx['currency']} {tx['total_amount']:.2f}</div>
                <div class="mb-3 small text-muted">Recipient: {tx['phone_number']} (Ref: {reference})</div>
                <form method="POST">
                    <button type="submit" class="btn btn-primary w-100 py-2 fw-bold">
                        Approve & Pay {tx['currency']} {tx['total_amount']:.2f}
                    </button>
                </form>
            </div>
        </body>
        </html>
        """

    # =========================================================================
    # 4. ADMIN DASHBOARD & MANAGEMENT
    # =========================================================================
    @app.route("/")
    def index():
        if session.get("is_admin"):
            return redirect(url_for("admin_dashboard"))
        return redirect(url_for("admin_login"))

    @app.route("/admin/login", methods=["GET", "POST"])
    def admin_login():
        if session.get("is_admin"):
            return redirect(url_for("admin_dashboard"))

        if request.method == "POST":
            client_ip = request.remote_addr or "127.0.0.1"
            if not admin_limiter.is_allowed(client_ip):
                flash("Too many login attempts. Please wait 1 minute.", "danger")
                return render_template("login.html")

            username = request.form.get("username", "")
            password = request.form.get("password", "")

            if verify_admin_credentials(username, password, client_ip):
                session["is_admin"] = True
                session["admin_user"] = username
                flash("Welcome to Tithe Storehouse Treasury Console.", "success")
                next_url = request.args.get("next")
                return redirect(next_url or url_for("admin_dashboard"))
            else:
                flash("Invalid admin credentials. Please check your username and password.", "danger")

        return render_template("login.html")

    @app.route("/admin/logout")
    def admin_logout():
        user = session.get("admin_user", "admin")
        session.clear()
        log_audit(actor=user, action="ADMIN_LOGOUT", ip_address=request.remote_addr)
        flash("You have been signed out safely.", "info")
        return redirect(url_for("admin_login"))

    @app.route("/admin/dashboard")
    @login_required
    def admin_dashboard():
        with get_db() as conn:
            cursor = conn.cursor()

            # Overall Net, Fee, Gross
            cursor.execute("""
                SELECT
                    COALESCE(SUM(amount), 0.0) as net_amount,
                    COALESCE(SUM(fee), 0.0) as fee_amount,
                    COALESCE(SUM(total_amount), 0.0) as gross_amount,
                    COUNT(*) as total_successful_count
                FROM transactions
                WHERE status = 'success'
            """)
            overall = cursor.fetchone()

            cursor.execute("SELECT COUNT(*) FROM members")
            member_count = cursor.fetchone()[0]

            metrics = {
                "net_amount": float(overall["net_amount"]),
                "fee_amount": float(overall["fee_amount"]),
                "gross_amount": float(overall["gross_amount"]),
                "total_successful_count": overall["total_successful_count"],
                "member_count": member_count
            }

            # Totals by Day, Week, Month
            today_str = date.today().isoformat()
            week_ago_str = (date.today() - timedelta(days=7)).isoformat()
            month_start_str = date.today().replace(day=1).isoformat()

            # Today
            cursor.execute("""
                SELECT COALESCE(SUM(amount), 0.0) as net, COALESCE(SUM(fee), 0.0) as fee, COUNT(*) as cnt
                FROM transactions WHERE status = 'success' AND date(created_at) = date(?)
            """, (today_str,))
            r_today = cursor.fetchone()

            # This Week
            cursor.execute("""
                SELECT COALESCE(SUM(amount), 0.0) as net, COALESCE(SUM(fee), 0.0) as fee, COUNT(*) as cnt
                FROM transactions WHERE status = 'success' AND date(created_at) >= date(?)
            """, (week_ago_str,))
            r_week = cursor.fetchone()

            # This Month
            cursor.execute("""
                SELECT COALESCE(SUM(amount), 0.0) as net, COALESCE(SUM(fee), 0.0) as fee, COUNT(*) as cnt
                FROM transactions WHERE status = 'success' AND date(created_at) >= date(?)
            """, (month_start_str,))
            r_month = cursor.fetchone()

            time_totals = {
                "today_net": float(r_today["net"]),
                "today_fee": float(r_today["fee"]),
                "today_count": r_today["cnt"],
                "week_net": float(r_week["net"]),
                "week_fee": float(r_week["fee"]),
                "week_count": r_week["cnt"],
                "month_net": float(r_month["net"]),
                "month_fee": float(r_month["fee"]),
                "month_count": r_month["cnt"]
            }

            # Totals by Fund
            cursor.execute("""
                SELECT f.code as fund_code, f.name as fund_name,
                       COALESCE(SUM(t.amount), 0.0) as total_net,
                       COUNT(t.id) as count
                FROM funds f
                LEFT JOIN transactions t ON f.code = t.fund_code AND t.status = 'success'
                GROUP BY f.code, f.name
                ORDER BY total_net DESC
            """)
            fund_totals = [dict(r) for r in cursor.fetchall()]

            # Totals by Member Community Group
            cursor.execute("""
                SELECT m.community_group, COUNT(DISTINCT m.id) as member_count,
                       COALESCE(SUM(t.amount), 0.0) as total_given
                FROM members m
                LEFT JOIN transactions t ON m.id = t.member_id AND t.status = 'success'
                GROUP BY m.community_group
                ORDER BY total_given DESC
            """)
            groups_data = [dict(r) for r in cursor.fetchall()]

            # Recent 10 Transactions
            cursor.execute("""
                SELECT t.*, m.name as member_name
                FROM transactions t
                LEFT JOIN members m ON t.member_id = m.id
                ORDER BY t.id DESC LIMIT 10
            """)
            recent_transactions = [dict(r) for r in cursor.fetchall()]

        return render_template(
            "dashboard.html",
            metrics=metrics,
            time_totals=time_totals,
            fund_totals=fund_totals,
            groups_data=groups_data,
            recent_transactions=recent_transactions
        )

    @app.route("/admin/members")
    @login_required
    def admin_members():
        q = request.args.get("q", "").strip()
        with get_db() as conn:
            cursor = conn.cursor()
            query = """
                SELECT m.*,
                       COALESCE(SUM(CASE WHEN t.status = 'success' THEN t.amount ELSE 0 END), 0.0) as total_given,
                       COALESCE(SUM(CASE WHEN t.status = 'success' AND t.fund_code = 'tithe' THEN t.amount ELSE 0 END), 0.0) as total_tithe,
                       COALESCE(SUM(CASE WHEN t.status = 'success' AND t.fund_code = 'offering' THEN t.amount ELSE 0 END), 0.0) as total_offering
                FROM members m
                LEFT JOIN transactions t ON m.id = t.member_id
            """
            params = []
            if q:
                query += " WHERE m.name LIKE ? OR m.phone_number LIKE ? OR m.community_group LIKE ?"
                wildcard = f"%{q}%"
                params = [wildcard, wildcard, wildcard]

            query += " GROUP BY m.id ORDER BY total_given DESC"
            cursor.execute(query, params)
            members = [dict(r) for r in cursor.fetchall()]

        return render_template("members.html", members=members, search_query=q)

    @app.route("/admin/members/<int:member_id>")
    @login_required
    def admin_member_detail(member_id):
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM members WHERE id = ?", (member_id,))
            member = cursor.fetchone()
            if not member:
                flash("Member record not found.", "warning")
                return redirect(url_for("admin_members"))

            cursor.execute("""
                SELECT * FROM transactions
                WHERE member_id = ?
                ORDER BY id DESC
            """, (member_id,))
            transactions = [dict(r) for r in cursor.fetchall()]

            cursor.execute("""
                SELECT
                    COALESCE(SUM(CASE WHEN status = 'success' THEN amount ELSE 0 END), 0.0) as total_given,
                    COALESCE(SUM(CASE WHEN status = 'success' AND fund_code = 'tithe' THEN amount ELSE 0 END), 0.0) as total_tithe,
                    COALESCE(SUM(CASE WHEN status = 'success' AND fund_code = 'offering' THEN amount ELSE 0 END), 0.0) as total_offering,
                    COALESCE(SUM(CASE WHEN status = 'success' AND fund_code NOT IN ('tithe', 'offering') THEN amount ELSE 0 END), 0.0) as total_special
                FROM transactions
                WHERE member_id = ?
            """, (member_id,))
            totals = dict(cursor.fetchone())

        return render_template("member_detail.html", member=dict(member), transactions=transactions, totals=totals)

    @app.route("/admin/members/add", methods=["POST"])
    @login_required
    def admin_add_member():
        name = request.form.get("name", "").strip()
        phone = request.form.get("phone_number", "").strip()
        group = request.form.get("community_group", "General Congregation").strip()

        if not phone:
            flash("Phone number is required.", "danger")
            return redirect(url_for("admin_members"))

        norm_phone = normalize_phone(phone)
        try:
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO members (name, phone_number, community_group)
                    VALUES (?, ?, ?)
                """, (name, norm_phone, group))
            log_audit(
                actor=session.get("admin_user", "admin"),
                action="MEMBER_REGISTERED",
                entity_type="member",
                entity_id=norm_phone,
                details={"name": name, "group": group}
            )
            flash(f"Member {name} registered successfully.", "success")
        except Exception as e:
            flash(f"Error registering member: {str(e)}", "danger")

        return redirect(url_for("admin_members"))

    @app.route("/admin/transactions")
    @login_required
    def admin_transactions():
        q = request.args.get("q", "").strip()
        fund = request.args.get("fund", "").strip()
        status = request.args.get("status", "").strip()

        with get_db() as conn:
            cursor = conn.cursor()
            query = """
                SELECT t.*, m.name as member_name
                FROM transactions t
                LEFT JOIN members m ON t.member_id = m.id
                WHERE 1=1
            """
            params = []
            if q:
                query += " AND (t.reference LIKE ? OR t.phone_number LIKE ? OR m.name LIKE ?)"
                wildcard = f"%{q}%"
                params.extend([wildcard, wildcard, wildcard])
            if fund:
                query += " AND t.fund_code = ?"
                params.append(fund)
            if status:
                query += " AND t.status = ?"
                params.append(status)

            query += " ORDER BY t.id DESC LIMIT 200"
            cursor.execute(query, params)
            transactions = [dict(r) for r in cursor.fetchall()]

        return render_template(
            "transactions.html",
            transactions=transactions,
            search_query=q,
            selected_fund=fund,
            selected_status=status
        )

    @app.route("/admin/export-csv")
    @login_required
    def admin_export_csv():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT t.id, t.reference, t.created_at, t.paid_at,
                       t.phone_number, m.name as member_name, m.community_group,
                       t.fund_code, f.name as fund_name,
                       t.amount, t.fee, t.total_amount, t.currency,
                       t.status, t.channel, t.sms_receipt_sent
                FROM transactions t
                LEFT JOIN members m ON t.member_id = m.id
                LEFT JOIN funds f ON t.fund_code = f.code
                ORDER BY t.id DESC
            """)
            rows = cursor.fetchall()

        si = io.StringIO()
        cw = csv.writer(si)
        cw.writerow([
            "ID", "Reference", "Created Date", "Paid Date",
            "Phone Number", "Member Name", "Ministry / Group",
            "Fund Code", "Fund Name",
            "Base Amount (Church Net)", "Processing Fee", "Total Donor Paid", "Currency",
            "Status", "Channel", "SMS Receipt Sent"
        ])

        for r in rows:
            cw.writerow([
                r["id"], r["reference"], r["created_at"], r["paid_at"] or "",
                r["phone_number"], r["member_name"] or "", r["community_group"] or "",
                r["fund_code"], r["fund_name"] or "",
                f"{r['amount']:.2f}", f"{r['fee']:.2f}", f"{r['total_amount']:.2f}", r["currency"],
                r["status"], r["channel"], "Yes" if r["sms_receipt_sent"] else "No"
            ])

        output = si.getvalue()
        log_audit(
            actor=session.get("admin_user", "admin"),
            action="CSV_EXPORTED",
            entity_type="transactions",
            details={"rows_exported": len(rows)}
        )

        filename = f"tithe_storehouse_export_{date.today().isoformat()}.csv"
        return Response(
            output,
            mimetype="text/csv",
            headers={"Content-Disposition": f"attachment; filename={filename}"}
        )

    @app.route("/admin/audit-logs")
    @login_required
    def admin_audit_logs():
        action_filter = request.args.get("action", "").strip()
        logs = get_audit_logs(limit=250, action_filter=action_filter)
        return render_template("audit_logs.html", logs=logs)

    @app.route("/admin/settings")
    @login_required
    def admin_settings():
        base, fee, total = calculate_charges(100)
        demo_fee = {"base": float(base), "fee": float(fee), "total": float(total)}
        return render_template("settings.html", demo_fee=demo_fee)

    @app.route("/admin/settings/test-ussd", methods=["POST"])
    @login_required
    def admin_test_ussd():
        phone = request.form.get("phone_number", "+233241112233")
        text = request.form.get("text", "")

        response = handle_ussd_request(
            session_id=f"sim_{int(datetime.now().timestamp())}",
            service_code=Config.CHURCH_SHORTCODE,
            phone_number=phone,
            text=text
        )

        base, fee, total = calculate_charges(100)
        demo_fee = {"base": float(base), "fee": float(fee), "total": float(total)}

        return render_template(
            "settings.html",
            demo_fee=demo_fee,
            test_phone=phone,
            test_text=text,
            test_response=response
        )

    @app.route("/admin/simulate/payment", methods=["POST"])
    @login_required
    def admin_simulate_payment():
        ref = request.form.get("reference", "").strip()
        if not ref:
            flash("Please enter a transaction reference.", "warning")
            return redirect(url_for("admin_settings"))

        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT t.*, m.name as member_name, f.name as fund_name
                FROM transactions t
                LEFT JOIN members m ON t.member_id = m.id
                LEFT JOIN funds f ON t.fund_code = f.code
                WHERE t.reference = ?
            """, (ref,))
            tx = cursor.fetchone()

            if not tx:
                flash(f"Reference '{ref}' not found in database.", "danger")
                return redirect(url_for("admin_settings"))

            cursor.execute("""
                UPDATE transactions
                SET status = 'success', paid_at = CURRENT_TIMESTAMP, sms_receipt_sent = 1
                WHERE reference = ?
            """, (ref,))

            # Send SMS receipt
            send_receipt_sms(
                to_phone=tx["phone_number"],
                member_name=tx["member_name"] or "",
                fund_name=tx["fund_name"] or tx["fund_code"].capitalize(),
                base_amount=tx["amount"],
                reference=ref
            )

            log_audit(
                actor=session.get("admin_user", "admin"),
                action="MANUAL_SIMULATED_PAYMENT",
                entity_type="transaction",
                entity_id=ref,
                details={"amount": tx["amount"], "phone": tx["phone_number"]}
            )

        flash(f"Payment for {ref} confirmed! Status updated and receipt SMS sent.", "success")
        return redirect(url_for("admin_transactions"))

    @app.route("/admin/transactions/<int:tx_id>/resend-receipt", methods=["POST"])
    @login_required
    def admin_resend_receipt(tx_id):
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT t.*, m.name as member_name, f.name as fund_name
                FROM transactions t
                LEFT JOIN members m ON t.member_id = m.id
                LEFT JOIN funds f ON t.fund_code = f.code
                WHERE t.id = ?
            """, (tx_id,))
            tx = cursor.fetchone()

            if tx:
                send_receipt_sms(
                    to_phone=tx["phone_number"],
                    member_name=tx["member_name"] or "",
                    fund_name=tx["fund_name"] or tx["fund_code"].capitalize(),
                    base_amount=tx["amount"],
                    reference=tx["reference"]
                )
                cursor.execute("UPDATE transactions SET sms_receipt_sent = 1 WHERE id = ?", (tx_id,))
                flash(f"Receipt SMS resent to {tx['phone_number']}.", "success")
            else:
                flash("Transaction not found.", "warning")

        return redirect(url_for("admin_transactions"))

    return app

if __name__ == "__main__":
    app = create_app()
    app.run(host=Config.SERVER_HOST, port=Config.SERVER_PORT, debug=False)
