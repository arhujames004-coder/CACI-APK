import unittest
import os
import sqlite3
import tempfile
import hmac
import hashlib
from decimal import Decimal

# Set up test environment before importing
os.environ["DATABASE_PATH"] = ":memory:"
os.environ["PAYSTACK_SECRET_KEY"] = "sk_test_mock_secret_key_12345"
os.environ["FEE_TYPE"] = "PERCENT"
os.environ["FEE_VALUE"] = "1.95"
os.environ["FEE_CAP"] = "25.00"

import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from tithe_storehouse.config import Config, calculate_charges
from tithe_storehouse.database import get_db, init_db, normalize_phone, get_or_create_member
from tithe_storehouse.audit import log_audit, get_audit_logs
from tithe_storehouse.ussd_handler import handle_ussd_request
from tithe_storehouse.paystack_client import verify_paystack_signature
from tithe_storehouse.rate_limiter import RateLimiter

class TitheStorehouseTestCase(unittest.TestCase):
    def setUp(self):
        # Create a fresh isolated sqlite database file for each test
        self.temp_db = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.temp_db.close()
        Config.DATABASE_PATH = self.temp_db.name
        Config.FEE_TYPE = "PERCENT"
        Config.FEE_VALUE = Decimal("1.95")
        Config.FEE_CAP = Decimal("25.00")
        init_db(seed=True)

    def tearDown(self):
        if os.path.exists(self.temp_db.name):
            os.unlink(self.temp_db.name)

    # -------------------------------------------------------------
    # 1. CHARGE & FEE HANDLING TESTS
    # -------------------------------------------------------------
    def test_fee_calculation_percent(self):
        Config.FEE_TYPE = "PERCENT"
        Config.FEE_VALUE = Decimal("1.95")
        base, fee, total = calculate_charges("100.00")
        self.assertEqual(base, Decimal("100.00"))
        self.assertEqual(fee, Decimal("1.95"))
        self.assertEqual(total, Decimal("101.95"))

    def test_fee_calculation_flat(self):
        Config.FEE_TYPE = "FLAT"
        Config.FEE_VALUE = Decimal("2.50")
        base, fee, total = calculate_charges(50)
        self.assertEqual(base, Decimal("50.00"))
        self.assertEqual(fee, Decimal("2.50"))
        self.assertEqual(total, Decimal("52.50"))

    def test_fee_cap(self):
        Config.FEE_TYPE = "PERCENT"
        Config.FEE_VALUE = Decimal("10.00")
        Config.FEE_CAP = Decimal("20.00")
        base, fee, total = calculate_charges(500)
        self.assertEqual(fee, Decimal("20.00"))  # capped at 20.00
        self.assertEqual(total, Decimal("520.00"))

    def test_invalid_amounts(self):
        with self.assertRaises(ValueError):
            calculate_charges("-10")
        with self.assertRaises(ValueError):
            calculate_charges("abc")

    # -------------------------------------------------------------
    # 2. AUDIT LOG & IMMUTABILITY TESTS
    # -------------------------------------------------------------
    def test_audit_log_append_only(self):
        log_id = log_audit("admin", "TEST_ACTION", "system", "1", {"info": "test"}, "127.0.0.1")
        self.assertIsNotNone(log_id)

        logs = get_audit_logs()
        self.assertTrue(any(l["action"] == "TEST_ACTION" for l in logs))

        # Test trigger: Attempting to UPDATE an audit record must FAIL
        with self.assertRaises(sqlite3.DatabaseError):
            with get_db() as conn:
                conn.execute("UPDATE audit_logs SET action = 'TAMPERED' WHERE id = ?", (log_id,))

        # Test trigger: Attempting to DELETE an audit record must FAIL
        with self.assertRaises(sqlite3.DatabaseError):
            with get_db() as conn:
                conn.execute("DELETE FROM audit_logs WHERE id = ?", (log_id,))

    # -------------------------------------------------------------
    # 3. MEMBER RECORDS & NORMALIZATION
    # -------------------------------------------------------------
    def test_phone_normalization(self):
        self.assertEqual(normalize_phone("0241112233"), "+233241112233")
        self.assertEqual(normalize_phone("+233241112233"), "+233241112233")

    def test_get_or_create_member(self):
        m = get_or_create_member("0249998877", name="Elder Timothy", community_group="Council of Elders")
        self.assertEqual(m["phone_number"], "+233249998877")
        self.assertEqual(m["name"], "Elder Timothy")

        # Second call returns existing member
        m2 = get_or_create_member("+233249998877")
        self.assertEqual(m2["id"], m["id"])

    # -------------------------------------------------------------
    # 4. USSD MENU SESSIONS & STATE MACHINE
    # -------------------------------------------------------------
    def test_ussd_root_menu(self):
        res = handle_ussd_request("sess-1", "*384*100#", "+233241112233", "")
        self.assertTrue(res.startswith("CON "))
        self.assertIn("1. Give Tithe", res)
        self.assertIn("2. Give Offering", res)
        self.assertIn("3. Building/Special Fund", res)
        self.assertIn("4. My Giving History", res)
        self.assertIn("5. Contact Church", res)

    def test_ussd_give_tithe_flow(self):
        phone = "+233241112233"
        # Step 1: User selects 1 (Give Tithe)
        s1 = handle_ussd_request("sess-1", "*384*100#", phone, "1")
        self.assertTrue(s1.startswith("CON "))
        self.assertIn("Enter Tithe Amount", s1)

        # Step 2: User enters amount "100"
        s2 = handle_ussd_request("sess-1", "*384*100#", phone, "1*100")
        self.assertTrue(s2.startswith("CON "))
        self.assertIn("100.00", s2)
        self.assertIn("Fee:", s2)
        self.assertIn("1. Confirm & Pay", s2)

        # Step 3: User confirms "1"
        s3 = handle_ussd_request("sess-1", "*384*100#", phone, "1*100*1")
        self.assertTrue(s3.startswith("END "))
        self.assertIn("created", s3.lower())
        self.assertIn("Ref:", s3)

        # Verify transaction created in database
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM transactions WHERE phone_number = ? AND fund_code = 'tithe' ORDER BY id DESC LIMIT 1", (phone,))
            tx = cursor.fetchone()
            self.assertIsNotNone(tx)
            self.assertEqual(tx["amount"], 100.0)
            self.assertEqual(tx["status"], "pending")

    def test_ussd_give_tithe_cancel(self):
        phone = "+233241112233"
        res = handle_ussd_request("sess-1", "*384*100#", phone, "1*50*2")
        self.assertTrue(res.startswith("END "))
        self.assertIn("cancelled", res.lower())

    def test_ussd_building_fund_flow(self):
        phone = "+233202223344"
        # Step 1: Select special fund menu
        s1 = handle_ussd_request("sess-2", "*384*100#", phone, "3")
        self.assertTrue(s1.startswith("CON "))
        self.assertIn("1. Building Fund", s1)

        # Step 2: Select Building Fund (1)
        s2 = handle_ussd_request("sess-2", "*384*100#", phone, "3*1")
        self.assertTrue(s2.startswith("CON "))
        self.assertIn("Enter Amount", s2)

        # Step 3: Enter amount "200"
        s3 = handle_ussd_request("sess-2", "*384*100#", phone, "3*1*200")
        self.assertTrue(s3.startswith("CON "))
        self.assertIn("200.00", s3)

        # Step 4: Confirm
        s4 = handle_ussd_request("sess-2", "*384*100#", phone, "3*1*200*1")
        self.assertTrue(s4.startswith("END "))
        self.assertIn("created", s4.lower())

    def test_ussd_giving_history(self):
        phone = "+233241112233"
        res = handle_ussd_request("sess-3", "*384*100#", phone, "4")
        self.assertTrue(res.startswith("END "))
        self.assertIn("Recent Giving", res)

    def test_ussd_contact_church(self):
        res = handle_ussd_request("sess-4", "*384*100#", "+233240000000", "5")
        self.assertTrue(res.startswith("END "))
        self.assertIn("Helpline", res)

    # -------------------------------------------------------------
    # 5. PAYSTACK WEBHOOK SIGNATURE VERIFICATION
    # -------------------------------------------------------------
    def test_paystack_hmac_verification(self):
        secret = "sk_test_mock_secret_key_12345"
        Config.PAYSTACK_WEBHOOK_SECRET = secret
        payload = b'{"event":"charge.success","data":{"reference":"TS-123","amount":10000}}'
        valid_signature = hmac.new(secret.encode("utf-8"), payload, hashlib.sha512).hexdigest()

        self.assertTrue(verify_paystack_signature(payload, valid_signature))
        self.assertFalse(verify_paystack_signature(payload, "invalid_sig_abc123"))

    # -------------------------------------------------------------
    # 6. RATE LIMITER
    # -------------------------------------------------------------
    def test_rate_limiter(self):
        limiter = RateLimiter(limit=3, window_seconds=10)
        self.assertTrue(limiter.is_allowed("user-1"))
        self.assertTrue(limiter.is_allowed("user-1"))
        self.assertTrue(limiter.is_allowed("user-1"))
        # 4th request within window must be rejected
        self.assertFalse(limiter.is_allowed("user-1"))
        # Different key still allowed
        self.assertTrue(limiter.is_allowed("user-2"))

if __name__ == "__main__":
    unittest.main()
