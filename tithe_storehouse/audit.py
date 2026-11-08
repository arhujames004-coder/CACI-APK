import json
from tithe_storehouse.database import get_db

def log_audit(actor, action, entity_type=None, entity_id=None, details=None, ip_address=None):
    """
    Append-only audit logger.
    Every payment step, webhook callback, and admin action is recorded here.
    SQLite triggers prevent updates and deletions.
    """
    if details is not None and not isinstance(details, str):
        try:
            details_str = json.dumps(details, default=str)
        except Exception:
            details_str = str(details)
    else:
        details_str = details or "{}"

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO audit_logs (actor, action, entity_type, entity_id, details, ip_address)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (
            str(actor),
            str(action),
            str(entity_type) if entity_type else None,
            str(entity_id) if entity_id else None,
            details_str,
            str(ip_address) if ip_address else "internal"
        ))
        return cursor.lastrowid

def get_audit_logs(limit=100, offset=0, action_filter=None):
    with get_db() as conn:
        cursor = conn.cursor()
        if action_filter:
            cursor.execute("""
                SELECT * FROM audit_logs
                WHERE action LIKE ?
                ORDER BY id DESC LIMIT ? OFFSET ?
            """, (f"%{action_filter}%", limit, offset))
        else:
            cursor.execute("""
                SELECT * FROM audit_logs
                ORDER BY id DESC LIMIT ? OFFSET ?
            """, (limit, offset))
        return [dict(r) for r in cursor.fetchall()]
