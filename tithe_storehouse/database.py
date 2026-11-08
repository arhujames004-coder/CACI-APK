import sqlite3
import json
import os
from contextlib import contextmanager
from tithe_storehouse.config import Config

def get_db_path():
    path = Config.DATABASE_PATH
    if not os.path.isabs(path):
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path = os.path.join(base_dir, path)
    return path

@contextmanager
def get_db():
    conn = sqlite3.connect(get_db_path(), timeout=15)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON;")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

def init_db(seed=True):
    """Initializes tables, indexes, immutability triggers, and initial seed data."""
    with get_db() as conn:
        cursor = conn.cursor()

        # Members table
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS members (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            phone_number TEXT UNIQUE NOT NULL,
            name TEXT,
            community_group TEXT DEFAULT 'General Congregation',
            city TEXT DEFAULT 'Accra',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """)

        # Funds table
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS funds (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            code TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL,
            description TEXT,
            is_active INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """)

        # Transactions table
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            reference TEXT UNIQUE NOT NULL,
            member_id INTEGER,
            phone_number TEXT NOT NULL,
            fund_code TEXT NOT NULL,
            amount REAL NOT NULL,
            fee REAL NOT NULL,
            total_amount REAL NOT NULL,
            currency TEXT DEFAULT 'GHS',
            status TEXT DEFAULT 'pending', -- pending, success, failed, abandoned
            channel TEXT DEFAULT 'ussd',
            city TEXT DEFAULT 'Accra',
            paystack_access_code TEXT,
            paystack_authorization_url TEXT,
            paid_at TIMESTAMP,
            sms_receipt_sent INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL,
            FOREIGN KEY (fund_code) REFERENCES funds(code)
        );
        """)

        # Audit logs table (Append-only)
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS audit_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            actor TEXT NOT NULL,
            action TEXT NOT NULL,
            entity_type TEXT,
            entity_id TEXT,
            details TEXT,
            ip_address TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """)

        # USSD Sessions (for tracking multi-step sessions)
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS ussd_sessions (
            session_id TEXT PRIMARY KEY,
            phone_number TEXT NOT NULL,
            current_step TEXT NOT NULL,
            session_data TEXT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """)

        # Indexes for fast search
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_members_phone ON members(phone_number);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_transactions_phone ON transactions(phone_number);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_transactions_ref ON transactions(reference);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_transactions_fund ON transactions(fund_code);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);")

        # Safely migrate existing databases if city column was missing
        try:
            cursor.execute("ALTER TABLE members ADD COLUMN city TEXT DEFAULT 'Accra';")
        except sqlite3.OperationalError:
            pass
        try:
            cursor.execute("ALTER TABLE transactions ADD COLUMN city TEXT DEFAULT 'Accra';")
        except sqlite3.OperationalError:
            pass

        # Immutability triggers on audit_logs: prevent UPDATE and DELETE
        cursor.execute("""
        CREATE TRIGGER IF NOT EXISTS trg_prevent_audit_update
        BEFORE UPDATE ON audit_logs
        BEGIN
            SELECT RAISE(ABORT, 'Audit log entries are strictly immutable and cannot be updated.');
        END;
        """)

        cursor.execute("""
        CREATE TRIGGER IF NOT EXISTS trg_prevent_audit_delete
        BEFORE DELETE ON audit_logs
        BEGIN
            SELECT RAISE(ABORT, 'Audit log entries are strictly immutable and cannot be deleted.');
        END;
        """)

        # Seed Funds if empty
        cursor.execute("SELECT COUNT(*) FROM funds")
        if cursor.fetchone()[0] == 0:
            funds_data = [
                ("tithe", "Tithe", "Scriptural 10% giving to God's storehouse"),
                ("offering", "Sunday Offering", "General ministry and church Sunday offering"),
                ("building", "Building Fund", "Sanctuary expansion and construction project"),
                ("missions", "Missions & Outreach", "Rural church planting and outreach fund"),
                ("welfare", "Benevolence & Welfare", "Care for widows, orphans, and needy members")
            ]
            cursor.executemany("INSERT INTO funds (code, name, description) VALUES (?, ?, ?)", funds_data)

        # Seed Sample Members if empty and seed requested
        if seed:
            cursor.execute("SELECT COUNT(*) FROM members")
            if cursor.fetchone()[0] == 0:
                sample_members = [
                    ("+233241112233", "Brother Emmanuel Mensah", "Men of Valor"),
                    ("+233202223344", "Sister Grace Osei", "Women of Grace"),
                    ("+233543334455", "Samuel Kofi Addo", "Youth Ministry"),
                    ("+233274445566", "Deaconess Mary Baah", "Praise Choir"),
                    ("+233245556677", "Elder David Quaye", "Council of Elders")
                ]
                cursor.executemany(
                    "INSERT INTO members (phone_number, name, community_group) VALUES (?, ?, ?)",
                    sample_members
                )

                # Seed sample recorded historical tithes/offerings
                sample_tx = [
                    ("TS-INIT-001", 1, "+233241112233", "tithe", 200.0, 3.90, 203.90, "success", "2026-10-25 09:15:00", 1),
                    ("TS-INIT-002", 2, "+233202223344", "offering", 50.0, 0.98, 50.98, "success", "2026-10-25 09:40:00", 1),
                    ("TS-INIT-003", 3, "+233543334455", "building", 100.0, 1.95, 101.95, "success", "2026-11-01 10:12:00", 1),
                    ("TS-INIT-004", 1, "+233241112233", "tithe", 250.0, 4.88, 254.88, "success", "2026-11-01 08:50:00", 1),
                    ("TS-INIT-005", 4, "+233274445566", "tithe", 150.0, 2.93, 152.93, "success", "2026-11-02 14:20:00", 1),
                ]
                cursor.executemany("""
                    INSERT INTO transactions (
                        reference, member_id, phone_number, fund_code, amount, fee,
                        total_amount, status, paid_at, sms_receipt_sent
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, sample_tx)

                # Log initial audit trail
                cursor.execute("""
                    INSERT INTO audit_logs (actor, action, entity_type, entity_id, details, ip_address)
                    VALUES ('system', 'SYSTEM_INITIALIZED', 'database', 'schema', '{"message": "Database and seed tables created with immutable audit triggers"}', '127.0.0.1')
                """)

def normalize_phone(phone):
    """Standardizes phone numbers to international format (e.g., +233...)."""
    if not phone:
        return ""
    p = phone.strip().replace(" ", "").replace("-", "")
    if p.startswith("0") and len(p) == 10:
        # Ghana standard: replace 0 with +233
        p = "+233" + p[1:]
    elif not p.startswith("+"):
        p = "+" + p
    return p

def get_or_create_member(phone, name=None, community_group="General Congregation"):
    phone = normalize_phone(phone)
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM members WHERE phone_number = ?", (phone,))
        row = cursor.fetchone()
        if row:
            if name and not row["name"]:
                cursor.execute(
                    "UPDATE members SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    (name, row["id"])
                )
            return dict(row)

        cursor.execute("""
            INSERT INTO members (phone_number, name, community_group)
            VALUES (?, ?, ?)
        """, (phone, name, community_group))
        member_id = cursor.lastrowid
        cursor.execute("SELECT * FROM members WHERE id = ?", (member_id,))
        return dict(cursor.fetchone())
