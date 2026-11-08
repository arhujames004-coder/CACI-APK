import uuid
from decimal import Decimal
from datetime import datetime
from tithe_storehouse.config import Config, calculate_charges
from tithe_storehouse.database import get_db, normalize_phone, get_or_create_member
from tithe_storehouse.paystack_client import initialize_payment
from tithe_storehouse.sms_service import send_payment_link_sms
from tithe_storehouse.audit import log_audit

def generate_reference(prefix="TS"):
    return f"{prefix}-{datetime.now().strftime('%y%m%d')}-{uuid.uuid4().hex[:6].upper()}"

def handle_ussd_request(session_id, service_code, phone_number, text):
    """
    Core Africa's Talking USSD state machine.
    Returns strings strictly beginning with 'CON ' or 'END '.
    """
    phone = normalize_phone(phone_number)
    text_clean = text.strip() if text else ""
    parts = text_clean.split("*") if text_clean else []

    # -------------------------------------------------------------
    # 0. ROOT MENU
    # -------------------------------------------------------------
    if not text_clean:
        return (
            f"CON Welcome to {Config.CHURCH_NAME}\n"
            "1. Give Tithe\n"
            "2. Give Offering\n"
            "3. Building/Special Fund\n"
            "4. My Giving History\n"
            "5. Contact Church"
        )

    # -------------------------------------------------------------
    # 1. GIVE TITHE
    # -------------------------------------------------------------
    if parts[0] == "1":
        if len(parts) == 1:
            return f"CON Enter Tithe Amount in {Config.CURRENCY}:"

        if len(parts) == 2:
            amount_str = parts[1].strip()
            try:
                base, fee, total = calculate_charges(amount_str)
            except Exception:
                return "END Invalid amount. Please enter a valid number (e.g. 100)."

            return (
                f"CON Giving {Config.CURRENCY} {base:.2f} Tithe\n"
                f"Processing Fee: {Config.CURRENCY} {fee:.2f}\n"
                f"Total to Pay: {Config.CURRENCY} {total:.2f}\n\n"
                "1. Confirm & Pay\n"
                "2. Cancel"
            )

        if len(parts) == 3:
            confirm = parts[2].strip()
            if confirm == "2":
                return "END Giving cancelled. May the Lord bless and keep you!"
            elif confirm != "1":
                return "END Invalid option. Session ended."

            amount_str = parts[1].strip()
            try:
                base, fee, total = calculate_charges(amount_str)
            except Exception:
                return "END Error calculating charges. Please try again."

            ref = generate_reference("TS-T")
            member = get_or_create_member(phone)

            # Record pending transaction
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO transactions (
                        reference, member_id, phone_number, fund_code,
                        amount, fee, total_amount, currency, status, channel
                    ) VALUES (?, ?, ?, 'tithe', ?, ?, ?, ?, 'pending', 'ussd')
                """, (ref, member["id"], phone, float(base), float(fee), float(total), Config.CURRENCY))

            # Initialize Paystack Payment URL
            init_res = initialize_payment(
                reference=ref,
                amount_total=total,
                email_or_phone=phone,
                metadata={
                    "fund": "tithe",
                    "member_id": member["id"],
                    "phone": phone,
                    "base_amount": float(base),
                    "fee": float(fee)
                }
            )

            pay_url = init_res.get("data", {}).get("authorization_url", f"{Config.BASE_URL}/pay/{ref}")

            # Send SMS with payment link
            send_payment_link_sms(
                to_phone=phone,
                fund_name="Tithe",
                base_amount=base,
                fee_amount=fee,
                total_amount=total,
                payment_url=pay_url,
                reference=ref
            )

            log_audit(
                actor="ussd",
                action="TITHE_INITIATED",
                entity_type="transaction",
                entity_id=ref,
                details={
                    "phone": phone,
                    "amount": float(base),
                    "fee": float(fee),
                    "total": float(total)
                }
            )

            return (
                f"END Tithe request of {Config.CURRENCY} {total:.2f} created!\n"
                "You will receive an SMS with payment instructions.\n"
                f"Ref: {ref}"
            )

    # -------------------------------------------------------------
    # 2. GIVE OFFERING
    # -------------------------------------------------------------
    elif parts[0] == "2":
        if len(parts) == 1:
            return f"CON Enter Offering Amount in {Config.CURRENCY}:"

        if len(parts) == 2:
            amount_str = parts[1].strip()
            try:
                base, fee, total = calculate_charges(amount_str)
            except Exception:
                return "END Invalid amount. Please enter a valid number (e.g. 50)."

            return (
                f"CON Giving {Config.CURRENCY} {base:.2f} Offering\n"
                f"Processing Fee: {Config.CURRENCY} {fee:.2f}\n"
                f"Total to Pay: {Config.CURRENCY} {total:.2f}\n\n"
                "1. Confirm & Pay\n"
                "2. Cancel"
            )

        if len(parts) == 3:
            confirm = parts[2].strip()
            if confirm == "2":
                return "END Offering cancelled. God bless you!"
            elif confirm != "1":
                return "END Invalid option. Session ended."

            amount_str = parts[1].strip()
            try:
                base, fee, total = calculate_charges(amount_str)
            except Exception:
                return "END Error calculating charges. Please try again."

            ref = generate_reference("TS-O")
            member = get_or_create_member(phone)

            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO transactions (
                        reference, member_id, phone_number, fund_code,
                        amount, fee, total_amount, currency, status, channel
                    ) VALUES (?, ?, ?, 'offering', ?, ?, ?, ?, 'pending', 'ussd')
                """, (ref, member["id"], phone, float(base), float(fee), float(total), Config.CURRENCY))

            init_res = initialize_payment(
                reference=ref,
                amount_total=total,
                email_or_phone=phone,
                metadata={"fund": "offering", "member_id": member["id"], "phone": phone}
            )

            pay_url = init_res.get("data", {}).get("authorization_url", f"{Config.BASE_URL}/pay/{ref}")

            send_payment_link_sms(
                to_phone=phone,
                fund_name="Sunday Offering",
                base_amount=base,
                fee_amount=fee,
                total_amount=total,
                payment_url=pay_url,
                reference=ref
            )

            log_audit(
                actor="ussd",
                action="OFFERING_INITIATED",
                entity_type="transaction",
                entity_id=ref,
                details={"phone": phone, "amount": float(base), "fee": float(fee)}
            )

            return (
                f"END Offering request of {Config.CURRENCY} {total:.2f} created!\n"
                "You will receive an SMS with payment instructions.\n"
                f"Ref: {ref}"
            )

    # -------------------------------------------------------------
    # 3. BUILDING / SPECIAL FUND
    # -------------------------------------------------------------
    elif parts[0] == "3":
        funds_map = {
            "1": ("building", "Building Fund"),
            "2": ("missions", "Missions & Outreach"),
            "3": ("welfare", "Benevolence & Welfare")
        }

        if len(parts) == 1:
            return (
                "CON Select Special Fund:\n"
                "1. Building Fund\n"
                "2. Missions & Outreach\n"
                "3. Benevolence & Welfare\n"
                "0. Cancel"
            )

        if len(parts) == 2:
            fund_choice = parts[1].strip()
            if fund_choice == "0":
                return "END Request cancelled."
            if fund_choice not in funds_map:
                return "END Invalid fund selected. Please try again."
            _, fund_name = funds_map[fund_choice]
            return f"CON Enter Amount for {fund_name} ({Config.CURRENCY}):"

        if len(parts) == 3:
            fund_choice = parts[1].strip()
            if fund_choice not in funds_map:
                return "END Invalid fund selected."
            _, fund_name = funds_map[fund_choice]
            amount_str = parts[2].strip()
            try:
                base, fee, total = calculate_charges(amount_str)
            except Exception:
                return "END Invalid amount. Please enter a number."

            return (
                f"CON Giving {Config.CURRENCY} {base:.2f} to {fund_name}\n"
                f"Processing Fee: {Config.CURRENCY} {fee:.2f}\n"
                f"Total to Pay: {Config.CURRENCY} {total:.2f}\n\n"
                "1. Confirm & Pay\n"
                "2. Cancel"
            )

        if len(parts) == 4:
            confirm = parts[3].strip()
            if confirm == "2":
                return "END Donation cancelled. Thank you."
            elif confirm != "1":
                return "END Invalid option. Session ended."

            fund_choice = parts[1].strip()
            fund_code, fund_name = funds_map.get(fund_choice, ("special", "Special Fund"))
            amount_str = parts[2].strip()
            base, fee, total = calculate_charges(amount_str)

            ref = generate_reference(f"TS-{fund_code[:2].upper()}")
            member = get_or_create_member(phone)

            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO transactions (
                        reference, member_id, phone_number, fund_code,
                        amount, fee, total_amount, currency, status, channel
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'ussd')
                """, (ref, member["id"], phone, fund_code, float(base), float(fee), float(total), Config.CURRENCY))

            init_res = initialize_payment(
                reference=ref,
                amount_total=total,
                email_or_phone=phone,
                metadata={"fund": fund_code, "member_id": member["id"], "phone": phone}
            )

            pay_url = init_res.get("data", {}).get("authorization_url", f"{Config.BASE_URL}/pay/{ref}")

            send_payment_link_sms(
                to_phone=phone,
                fund_name=fund_name,
                base_amount=base,
                fee_amount=fee,
                total_amount=total,
                payment_url=pay_url,
                reference=ref
            )

            log_audit(
                actor="ussd",
                action="SPECIAL_FUND_INITIATED",
                entity_type="transaction",
                entity_id=ref,
                details={"fund": fund_code, "amount": float(base), "phone": phone}
            )

            return (
                f"END {fund_name} gift of {Config.CURRENCY} {total:.2f} created!\n"
                "Check your SMS for payment instructions.\n"
                f"Ref: {ref}"
            )

    # -------------------------------------------------------------
    # 4. MY GIVING HISTORY
    # -------------------------------------------------------------
    elif parts[0] == "4":
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT t.amount, t.fund_code, f.name as fund_name, t.paid_at, t.created_at
                FROM transactions t
                LEFT JOIN funds f ON t.fund_code = f.code
                WHERE t.phone_number = ? AND t.status = 'success'
                ORDER BY t.id DESC LIMIT 3
            """, (phone,))
            rows = cursor.fetchall()

            current_year = datetime.now().year
            cursor.execute("""
                SELECT SUM(amount) as ytd_total
                FROM transactions
                WHERE phone_number = ? AND status = 'success'
                AND strftime('%Y', created_at) = ?
            """, (phone, str(current_year)))
            ytd = cursor.fetchone()["ytd_total"] or 0.0

        if not rows:
            return (
                f"END Welcome to {Config.CHURCH_NAME}!\n"
                f"No previous giving recorded for {phone}.\n"
                "May God richly bless you as you give today!"
            )

        history_lines = []
        for r in rows:
            dt = r["paid_at"] or r["created_at"] or ""
            date_short = dt[5:10] if len(dt) >= 10 else ""
            fund_name = r["fund_name"] or r["fund_code"].capitalize()
            history_lines.append(f"• {fund_name}: {Config.CURRENCY} {r['amount']:.2f} ({date_short})")

        history_text = "\n".join(history_lines)
        return (
            f"END Your Recent Giving:\n"
            f"{history_text}\n"
            f"{current_year} Total: {Config.CURRENCY} {ytd:.2f}\n"
            "God bless your faithful stewardship!"
        )

    # -------------------------------------------------------------
    # 5. CONTACT CHURCH
    # -------------------------------------------------------------
    elif parts[0] == "5":
        return (
            f"END {Config.CHURCH_NAME}\n"
            f"Helpline: {Config.CHURCH_PHONE}\n"
            "Sunday Services: 7:00 AM & 9:30 AM\n"
            "Office: Mon - Fri, 8:00 AM - 4:30 PM\n"
            "May the peace of Christ abide with you!"
        )

    return f"END Invalid selection. Dial {Config.CHURCH_SHORTCODE} to start over."
