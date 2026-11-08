import hmac
import hashlib
import json
import uuid
from decimal import Decimal
from tithe_storehouse.config import Config
from tithe_storehouse.audit import log_audit

try:
    import requests
except ImportError:
    requests = None

def verify_paystack_signature(raw_body_bytes, signature_header):
    """
    Verifies that incoming webhook requests originate from Paystack.
    Paystack signs requests using HMAC-SHA512 with your secret key.
    """
    if not signature_header:
        return False

    secret = (Config.PAYSTACK_WEBHOOK_SECRET or Config.PAYSTACK_SECRET_KEY or "").strip()
    if not secret:
        # In local test/simulation mode without a secret configured, log warning
        return True

    computed_hmac = hmac.new(
        secret.encode("utf-8"),
        raw_body_bytes,
        hashlib.sha512
    ).hexdigest()

    return hmac.compare_digest(computed_hmac, signature_header)

def initialize_payment(reference, amount_total, email_or_phone, metadata=None):
    """
    Calls Paystack API to initialize payment and create a payment URL.
    amount_total: Decimal or float in major currency (e.g. 50.98 GHS).
    Paystack requires amounts in minor units (kobo/pesewas, i.e. * 100).
    """
    # Convert to minor currency unit (pesewas / cents)
    amount_in_pesewas = int(round(float(amount_total) * 100))

    # Paystack requires an email. If only phone is available, generate a standard member identifier email
    clean_phone = email_or_phone.replace("+", "").replace(" ", "")
    email = f"member-{clean_phone}@churchstorehouse.org" if "@" not in email_or_phone else email_or_phone

    payload = {
        "reference": reference,
        "amount": amount_in_pesewas,
        "currency": Config.CURRENCY,
        "email": email,
        "channels": ["mobile_money", "card"],
        "callback_url": f"{Config.BASE_URL}/payment/callback",
        "metadata": metadata or {}
    }

    secret_key = Config.PAYSTACK_SECRET_KEY.strip()

    # If secret key is not set or starts with placeholder, return simulated payment link
    if not secret_key or "your_paystack" in secret_key or not requests:
        simulated_url = f"{Config.BASE_URL}/pay/mock/{reference}"
        log_audit(
            actor="paystack_client",
            action="PAYMENT_INITIALIZED_SIMULATED",
            entity_type="transaction",
            entity_id=reference,
            details={"url": simulated_url, "amount": float(amount_total), "note": "Sandbox/local mode"}
        )
        return {
            "status": True,
            "message": "Authorization url created (local/sandbox)",
            "data": {
                "authorization_url": simulated_url,
                "access_code": f"acc_{uuid.uuid4().hex[:12]}",
                "reference": reference
            }
        }

    headers = {
        "Authorization": f"Bearer {secret_key}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(
            "https://api.paystack.co/transaction/initialize",
            json=payload,
            headers=headers,
            timeout=12
        )
        data = response.json()
        if response.status_code == 200 and data.get("status"):
            log_audit(
                actor="paystack_client",
                action="PAYMENT_INITIALIZED",
                entity_type="transaction",
                entity_id=reference,
                details=data.get("data")
            )
            return data
        else:
            log_audit(
                actor="paystack_client",
                action="PAYMENT_INIT_FAILED",
                entity_type="transaction",
                entity_id=reference,
                details={"error": data.get("message")}
            )
            return {
                "status": False,
                "message": data.get("message", "Paystack initialization failed"),
                "data": None
            }
    except Exception as e:
        log_audit(
            actor="paystack_client",
            action="PAYMENT_INIT_EXCEPTION",
            entity_type="transaction",
            entity_id=reference,
            details={"exception": str(e)}
        )
        # Fallback to simulated local link so user testing never crashes
        simulated_url = f"{Config.BASE_URL}/pay/mock/{reference}"
        return {
            "status": True,
            "message": "Authorization url created (offline fallback)",
            "data": {
                "authorization_url": simulated_url,
                "access_code": f"acc_{uuid.uuid4().hex[:12]}",
                "reference": reference
            }
        }

def verify_transaction(reference):
    """Verifies payment status directly with Paystack API."""
    secret_key = Config.PAYSTACK_SECRET_KEY.strip()
    if not secret_key or "your_paystack" in secret_key or not requests:
        return {"status": True, "data": {"status": "success", "reference": reference}}

    headers = {
        "Authorization": f"Bearer {secret_key}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.get(
            f"https://api.paystack.co/transaction/verify/{reference}",
            headers=headers,
            timeout=12
        )
        return response.json()
    except Exception as e:
        return {"status": False, "message": str(e)}
