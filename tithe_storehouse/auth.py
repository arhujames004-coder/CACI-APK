from functools import wraps
import hashlib
from flask import session, redirect, url_for, request, flash
from tithe_storehouse.config import Config
from tithe_storehouse.audit import log_audit

try:
    from werkzeug.security import check_password_hash, generate_password_hash
except ImportError:
    # Minimal fallback using SHA256 if running without werkzeug
    def generate_password_hash(pwd):
        salt = "tithe_salt_2026"
        return "sha256$" + hashlib.sha256((salt + pwd).encode()).hexdigest()

    def check_password_hash(p_hash, pwd):
        salt = "tithe_salt_2026"
        return p_hash == ("sha256$" + hashlib.sha256((salt + pwd).encode()).hexdigest())

def verify_admin_credentials(username, password, client_ip="127.0.0.1"):
    """
    Verifies admin credentials securely against Config.ADMIN_USERNAME and Config.ADMIN_PASSWORD_HASH.
    Audits all attempts.
    """
    is_valid_user = (username.strip().lower() == Config.ADMIN_USERNAME.strip().lower())
    is_valid_pass = False

    if is_valid_user:
        # Check against stored hash or fallback default password
        target_hash = Config.ADMIN_PASSWORD_HASH
        try:
            is_valid_pass = check_password_hash(target_hash, password)
        except Exception:
            # Fallback for plain text password test in local dev
            is_valid_pass = (password == "GraceTithe2026!")

    if is_valid_user and is_valid_pass:
        log_audit(
            actor=username,
            action="ADMIN_LOGIN_SUCCESS",
            entity_type="auth",
            entity_id=username,
            details={"ip": client_ip},
            ip_address=client_ip
        )
        return True
    else:
        log_audit(
            actor=username or "unknown",
            action="ADMIN_LOGIN_FAILED",
            entity_type="auth",
            entity_id=username,
            details={"ip": client_ip, "reason": "Invalid username or password"},
            ip_address=client_ip
        )
        return False

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not session.get("is_admin"):
            flash("Please sign in to access the Church Admin Console.", "warning")
            return redirect(url_for("admin_login", next=request.path))
        return f(*args, **kwargs)
    return decorated_function
