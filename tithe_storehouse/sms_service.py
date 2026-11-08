import os
from tithe_storehouse.config import Config
from tithe_storehouse.audit import log_audit

try:
    import africastalking
except ImportError:
    africastalking = None

try:
    import requests
except ImportError:
    requests = None

# In-memory recent sent SMS for local inspection / simulator UI
RECENT_SENT_SMS = []

def send_sms(to_phone, message):
    """
    Sends an SMS via Africa's Talking API.
    Handles sandbox, live API, and local test mock fallback.
    """
    username = (Config.AFRICASTALKING_USERNAME or "").strip()
    api_key = (Config.AFRICASTALKING_API_KEY or "").strip()
    sender_id = (Config.AFRICASTALKING_SENDER_ID or "").strip()

    # Record message for UI / testing
    sms_record = {
        "to": to_phone,
        "message": message,
        "timestamp": os.getenv("CURRENT_TIME", "Just now"),
        "status": "SENT"
    }
    RECENT_SENT_SMS.insert(0, sms_record)
    if len(RECENT_SENT_SMS) > 50:
        RECENT_SENT_SMS.pop()

    # If no live API key provided or placeholder, simulate successfully
    if not api_key or "your_africastalking" in api_key:
        log_audit(
            actor="sms_service",
            action="SMS_SENT_SIMULATED",
            entity_type="sms",
            entity_id=to_phone,
            details={"message": message, "to": to_phone, "mode": "sandbox/simulation"}
        )
        return {"status": "success", "mode": "simulated", "message": message}

    # Attempt live Africa's Talking dispatch
    if africastalking:
        try:
            africastalking.initialize(username, api_key)
            sms = africastalking.SMS
            recipients = [to_phone]
            kwargs = {"recipients": recipients, "message": message}
            if sender_id:
                kwargs["sender_id"] = sender_id
            response = sms.send(**kwargs)
            log_audit(
                actor="sms_service",
                action="SMS_SENT_AFRICASTALKING",
                entity_type="sms",
                entity_id=to_phone,
                details=response
            )
            return {"status": "success", "response": response}
        except Exception as e:
            log_audit(
                actor="sms_service",
                action="SMS_SEND_ERROR",
                entity_type="sms",
                entity_id=to_phone,
                details={"error": str(e)}
            )
            return {"status": "error", "message": str(e)}

    # Fallback direct HTTP if SDK is not installed
    if requests:
        try:
            url = "https://api.africastalking.com/version1/messaging"
            if username.lower() == "sandbox":
                url = "https://api.sandbox.africastalking.com/version1/messaging"
            headers = {
                "ApiKey": api_key,
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json"
            }
            data = {
                "username": username,
                "to": to_phone,
                "message": message
            }
            if sender_id:
                data["from"] = sender_id
            res = requests.post(url, data=data, headers=headers, timeout=10)
            log_audit(
                actor="sms_service",
                action="SMS_SENT_HTTP",
                entity_type="sms",
                entity_id=to_phone,
                details=res.text
            )
            return {"status": "success", "raw": res.text}
        except Exception as e:
            log_audit(
                actor="sms_service",
                action="SMS_HTTP_ERROR",
                entity_type="sms",
                entity_id=to_phone,
                details={"error": str(e)}
            )
            return {"status": "error", "message": str(e)}

    return {"status": "simulated", "message": message}

def send_payment_link_sms(to_phone, fund_name, base_amount, fee_amount, total_amount, payment_url, reference):
    """
    Sends payment prompt/link SMS after USSD initiation.
    Clearly shows base amount, processing fee, and payment link.
    """
    msg = (
        f"{Config.CHURCH_NAME}: Please complete your {fund_name} of "
        f"{Config.CURRENCY} {base_amount:.2f} (+ {Config.CURRENCY} {fee_amount:.2f} fee = "
        f"{Config.CURRENCY} {total_amount:.2f}).\n"
        f"Pay with MoMo or Card: {payment_url}\n"
        f"Ref: {reference}"
    )
    return send_sms(to_phone, msg)

def send_receipt_sms(to_phone, member_name, fund_name, base_amount, reference):
    """
    Requirement 4: Mark payment as paid, send an SMS receipt:
    'Thank you, GHS X tithe received. God bless you.'
    """
    name_str = f" {member_name}" if member_name else ""
    msg = (
        f"Thank you{name_str}! {Config.CURRENCY} {base_amount:.2f} {fund_name.lower()} received. "
        f"Ref: {reference}. God bless you! - {Config.CHURCH_NAME}"
    )
    return send_sms(to_phone, msg)
