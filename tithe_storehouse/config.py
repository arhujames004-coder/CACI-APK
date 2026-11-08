import os
from decimal import Decimal, ROUND_HALF_UP

# Simple manual .env parser if python-dotenv is not installed
def load_dotenv(filepath=".env"):
    if not os.path.exists(filepath):
        # Also check parent directory if running from subfolder
        parent_env = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", filepath)
        if os.path.exists(parent_env):
            filepath = parent_env
        else:
            return
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                k = k.strip()
                v = v.strip().strip("'").strip('"')
                if k not in os.environ:
                    os.environ[k] = v
    except Exception:
        pass

# Attempt to load .env
load_dotenv(".env")

class Config:
    CHURCH_NAME = os.getenv("CHURCH_NAME", "Christ Apostolic Church International")
    CHURCH_PHONE = os.getenv("CHURCH_PHONE", "+233240123456")
    CHURCH_SHORTCODE = os.getenv("CHURCH_SHORTCODE", "*384*100#")
    CURRENCY = os.getenv("CURRENCY", "GHS")

    SECRET_KEY = os.getenv("SECRET_KEY", "dev-secret-key-tithe-storehouse-change-in-prod")
    DATABASE_PATH = os.getenv("DATABASE_PATH", "tithe_storehouse.db")
    SERVER_HOST = os.getenv("SERVER_HOST", "0.0.0.0")
    SERVER_PORT = int(os.getenv("SERVER_PORT", "5000"))
    BASE_URL = os.getenv("BASE_URL", "http://localhost:5000")

    # Processing Fee Configuration
    FEE_TYPE = os.getenv("FEE_TYPE", "PERCENT").upper()  # PERCENT or FLAT
    FEE_VALUE = Decimal(os.getenv("FEE_VALUE", "1.95"))
    FEE_CAP = Decimal(os.getenv("FEE_CAP", "25.00")) if os.getenv("FEE_CAP") else None

    # Paystack
    PAYSTACK_SECRET_KEY = os.getenv("PAYSTACK_SECRET_KEY", "")
    PAYSTACK_PUBLIC_KEY = os.getenv("PAYSTACK_PUBLIC_KEY", "")
    PAYSTACK_WEBHOOK_SECRET = os.getenv("PAYSTACK_WEBHOOK_SECRET", PAYSTACK_SECRET_KEY)

    # Africa's Talking
    AFRICASTALKING_USERNAME = os.getenv("AFRICASTALKING_USERNAME", "sandbox")
    AFRICASTALKING_API_KEY = os.getenv("AFRICASTALKING_API_KEY", "")
    AFRICASTALKING_SENDER_ID = os.getenv("AFRICASTALKING_SENDER_ID", "")

    # Admin Dashboard
    ADMIN_USERNAME = os.getenv("ADMIN_USERNAME", "admin")
    # Default password hash for 'GraceTithe2026!'
    ADMIN_PASSWORD_HASH = os.getenv(
        "ADMIN_PASSWORD_HASH",
        "scrypt:32768:8:1$u7xV4F1o0hO$98e1f5798993f1bb1ad675276e0176df9d4b684cb3a7042a5a54b35e5e4905d5dbad4cb2c48dafa310d7eb599d123b379e49c716fe00742d45c60f274cb32d43"
    )

    # Rate Limiting
    RATE_LIMIT_PER_MINUTE = int(os.getenv("RATE_LIMIT_PER_MINUTE", "60"))

def calculate_charges(amount):
    """
    Given a base amount (float, int, or Decimal), calculates:
    (base_amount, fee_amount, total_amount)
    as 2-decimal rounded Decimals.
    """
    try:
        base = Decimal(str(amount)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except Exception:
        raise ValueError(f"Invalid amount format: {amount}")

    if base <= 0:
        raise ValueError("Amount must be greater than zero.")

    if Config.FEE_TYPE == "PERCENT":
        # Fee is (base * FEE_VALUE) / 100
        fee = (base * (Config.FEE_VALUE / Decimal("100"))).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    elif Config.FEE_TYPE == "FLAT":
        fee = Config.FEE_VALUE.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    else:
        fee = Decimal("0.00")

    if Config.FEE_CAP and fee > Config.FEE_CAP:
        fee = Config.FEE_CAP

    total = (base + fee).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return base, fee, total
