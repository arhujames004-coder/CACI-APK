# 🏛️ Tithe Storehouse

> **A secure, phone-first church tithe and offering collection system built with Python 3, Flask, SQLite, Africa's Talking (USSD/SMS), and Paystack.**

Designed specifically for local church administration, running natively in **Termux on Android** or deployable to any cheap VPS / cloud server (e.g. Ubuntu, DigitalOcean, Render, Fly.io).

---

## 📋 Features

1. **Africa's Talking USSD Menu (`*384*100#`)**:
   - `1. Give Tithe` — Prompt amount, transparent fee calculation, instant SMS payment prompt.
   - `2. Give Offering` — General Sunday collection with transparent fee preview.
   - `3. Building/Special Fund` — Dedicated funds (Building, Missions, Welfare).
   - `4. My Giving History` — Year-to-date total and recent 3 donations lookup by member phone.
   - `5. Contact Church` — Helpline numbers, office hours, and pastoral desk.
2. **Transparent Charge Handling**:
   - Configurable processing fee (`PERCENT` e.g. 1.95%, or `FLAT` e.g. GHS 1.50) set in `.env`.
   - Clear breakdown shown to the member on USSD screen before they confirm:
     `Giving GHS 100.00 | Fee: GHS 1.95 | Total to Pay: GHS 101.95`.
   - Base amount (church net) and fees are recorded separately in the ledger; never a hidden fee.
3. **Member Directory & Community Groups**:
   - Links giving records to phone numbers.
   - Tracks member name, phone, community/fellowship group (*Men of Valor*, *Women of Grace*, *Youth Ministry*, *Choir*, etc.).
   - Full individual giving statements with print support.
4. **Secure Paystack Webhook Handler**:
   - Verifies incoming HMAC-SHA512 `X-Paystack-Signature` against `PAYSTACK_SECRET_KEY`.
   - Marks transaction as `success` with timestamp.
   - Automatically dispatches SMS receipt via Africa's Talking:
     *"Thank you [Name]! GHS 100.00 tithe received. Ref: TS-T-XXXX. God bless you! - [Church Name]"*
5. **Mobile-Friendly Admin Dashboard (Password Protected)**:
   - Summary statistics: Total Gross, Processing Fees, Net to Church, Registered Donors.
   - Time totals: Today, This Week, This Month, All Time.
   - Totals by fund and giving by fellowship/community group.
   - Searchable transaction history with fee breakdown.
   - One-click CSV Export for church auditors and finance committees.
6. **Immutable Cryptographic Audit Trail**:
   - Every USSD interaction, fee calculation, webhook callback, and admin action is logged.
   - **Enforced at the SQLite database layer** with triggers preventing `UPDATE` or `DELETE` on `audit_logs`.

---

## 🗂️ Project Structure

```text
├── .env.example                       # Template for church, payment, and admin secrets
├── requirements.txt                   # Production Python dependencies
├── README.md                          # Documentation, Termux run guide & test plan
├── tithe_storehouse/
│   ├── app.py                         # Flask application & HTTP endpoints (USSD, Webhook, Admin)
│   ├── config.py                      # .env parsing and charge calculation logic
│   ├── database.py                    # SQLite schema, context managers, and seed data
│   ├── audit.py                       # Append-only audit logger
│   ├── ussd_handler.py                # Africa's Talking USSD state machine (CON/END)
│   ├── paystack_client.py             # Paystack payment initialization and HMAC verification
│   ├── sms_service.py                 # Africa's Talking SMS integration and receipts
│   ├── rate_limiter.py                # Sliding-window rate limiter (USSD, Webhook, Auth)
│   ├── auth.py                        # Password hashing and @login_required decorator
│   ├── run.py                         # Standalone launcher
│   ├── termux_setup.sh                # 1-command installer script for Termux
│   ├── templates/                     # Mobile-responsive Admin Dashboard templates
│   │   ├── base.html
│   │   ├── login.html
│   │   ├── dashboard.html
│   │   ├── members.html
│   │   ├── member_detail.html
│   │   ├── transactions.html
│   │   ├── audit_logs.html
│   │   └── settings.html
│   └── tests/
│       └── test_ussd_and_db.py        # Complete automated test suite
```

---

## 📱 Step-by-Step Run Instructions for Termux on Android

You can run this entire church treasury server directly on an Android phone using Termux.

### Step 1: Install Termux
Install Termux from [F-Droid](https://f-droid.org/packages/com.termux/) (avoid Google Play version as it is deprecated).

### Step 2: Open Termux and Install Required Packages
```bash
pkg update && pkg upgrade -y
pkg install -y python python-pip sqlite git openssl clang libffi
```

### Step 3: Clone or Copy Project
```bash
# If cloned from Git:
git clone <your-repo-url> tithe_storehouse
cd tithe_storehouse

# Or navigate to the folder where you copied the project:
cd tithe_storehouse
```

### Step 4: Run the Automated Setup Script
```bash
chmod +x tithe_storehouse/termux_setup.sh
bash tithe_storehouse/termux_setup.sh
```

### Manual Alternative (Without script):
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
python3 tithe_storehouse/run.py
```

### Step 5: Access Admin Dashboard on Phone
Open your Android browser (Chrome, Firefox) and navigate to:
`http://127.0.0.1:5000`
- **Default Username:** `admin`
- **Default Password:** `GraceTithe2026!`

*(To keep Termux running in the background on Android, pull down the Android notification shade and tap **Acquire Wakelock** on the Termux notification).*

---

## 🌐 Exposing the Webhook & USSD Locally (ngrok or Cloudflared)

For Africa's Talking and Paystack to communicate with your Termux or local server, you need a public HTTPS tunnel.

### Option A: Using Cloudflared (Free, No Account Needed)
1. In Termux, install cloudflared:
   ```bash
   pkg install cloudflared
   ```
2. Start the tunnel in a separate Termux session:
   ```bash
   cloudflared tunnel --url http://localhost:5000
   ```
3. Copy the generated `https://*.trycloudflare.com` URL.

### Option B: Using ngrok
1. In Termux or PC:
   ```bash
   # Termux:
   pkg install ngrok
   ngrok config add-authtoken <YOUR_NGROK_TOKEN>
   ngrok http 5000
   ```
2. Copy the forwarded HTTPS address (e.g. `https://church-tithe.ngrok-free.app`).

### Step C: Configure Telecom & Payment Portals

1. **Africa's Talking USSD**:
   - Go to **Africa's Talking Sandbox / Live Dashboard** -> **USSD** -> **Service Codes**.
   - Set Callback URL to:
     `https://your-tunnel-url.ngrok-free.app/ussd`
     *(Method: POST)*

2. **Paystack Webhook**:
   - Go to **Paystack Dashboard** -> **Settings** -> **Preferences** -> **Webhooks**.
   - Set Webhook URL to:
     `https://your-tunnel-url.ngrok-free.app/webhook/paystack`
   - Paste your Secret Key in `.env` as `PAYSTACK_SECRET_KEY` and `PAYSTACK_WEBHOOK_SECRET`.

---

## 🧪 Short Test Plan with Sample USSD Sessions

### Test Case 1: Initial Dial (Root Menu)
```bash
curl -X POST http://127.0.0.1:5000/ussd \
  -d "sessionId=test_sess_01" \
  -d "serviceCode=*384*100#" \
  -d "phoneNumber=+233241112233" \
  -d "text="
```
**Expected Response:**
```text
CON Welcome to Grace Baptist Church
1. Give Tithe
2. Give Offering
3. Building/Special Fund
4. My Giving History
5. Contact Church
```

---

### Test Case 2: Give Tithe (GHS 50 with Fee Preview)
**Step 2a: Member selects 1 (Tithe)**
```bash
curl -X POST http://127.0.0.1:5000/ussd \
  -d "sessionId=test_sess_02" \
  -d "serviceCode=*384*100#" \
  -d "phoneNumber=+233241112233" \
  -d "text=1"
```
**Expected Response:**
```text
CON Enter Tithe Amount in GHS:
```

**Step 2b: Member enters 50**
```bash
curl -X POST http://127.0.0.1:5000/ussd \
  -d "sessionId=test_sess_02" \
  -d "serviceCode=*384*100#" \
  -d "phoneNumber=+233241112233" \
  -d "text=1*50"
```
**Expected Response (Transparent Fee Breakdown):**
```text
CON Giving GHS 50.00 Tithe
Processing Fee: GHS 0.98
Total to Pay: GHS 50.98

1. Confirm & Pay
2. Cancel
```

**Step 2c: Member confirms 1**
```bash
curl -X POST http://127.0.0.1:5000/ussd \
  -d "sessionId=test_sess_02" \
  -d "serviceCode=*384*100#" \
  -d "phoneNumber=+233241112233" \
  -d "text=1*50*1"
```
**Expected Response:**
```text
END Tithe request of GHS 50.98 created!
You will receive an SMS with payment instructions.
Ref: TS-T-261104-XXXXXX
```

---

### Test Case 3: Simulate Paystack Webhook (Payment Receipt SMS)
Once the payment is approved on Mobile Money / Card, Paystack sends a webhook:
```bash
# Using the reference generated in Step 2c (e.g. TS-T-261104-XXXXXX):
curl -X POST http://127.0.0.1:5000/webhook/paystack \
  -H "Content-Type: application/json" \
  -H "X-Paystack-Signature: (configured secret or test mode)" \
  -d '{
    "event": "charge.success",
    "data": {
      "reference": "TS-T-261104-XXXXXX",
      "amount": 5098,
      "status": "success",
      "currency": "GHS"
    }
  }'
```
**Result:**
- Ledger marks status as `success`.
- Base gift (`GHS 50.00`) credited to Church Tithe Net.
- Processing fee (`GHS 0.98`) recorded under Fees Collected.
- Africa's Talking dispatches SMS:
  *"Thank you Brother Emmanuel Mensah! GHS 50.00 tithe received. Ref: TS-T-261104-XXXXXX. God bless you! - Grace Baptist Church"*
- Immutable entry written to `audit_logs`.

---

### Test Case 4: Member Giving History
```bash
curl -X POST http://127.0.0.1:5000/ussd \
  -d "sessionId=test_sess_04" \
  -d "serviceCode=*384*100#" \
  -d "phoneNumber=+233241112233" \
  -d "text=4"
```
**Expected Response:**
```text
END Your Recent Giving:
• Tithe: GHS 200.00 (10-25)
• Tithe: GHS 250.00 (11-01)
• Tithe: GHS 50.00 (11-04)
2026 Total: GHS 500.00
God bless your faithful stewardship!
```

---

## 🔒 Security Best Practices

1. **Zero Secret Leakage**:
   All tokens (`PAYSTACK_SECRET_KEY`, `AFRICASTALKING_API_KEY`, `SECRET_KEY`, `ADMIN_PASSWORD_HASH`) reside in `.env`.
2. **Cryptographic Webhook Signature**:
   Webhook requests verify HMAC-SHA512 against the raw request body. Spoofed webhooks are rejected with HTTP 400.
3. **No Card or PIN Data Handled**:
   Payment authorization happens strictly on telecom USSD push (Mobile Money prompt) or Paystack PCI-DSS certified checkout. No PIN or PAN numbers touch the server.
4. **Append-Only Immutability**:
   `audit_logs` has database-level SQLite triggers that abort any `UPDATE` or `DELETE` statements.
5. **Rate Limiting**:
   Protects USSD endpoints from telecom replay loops and protects admin authentication from brute-force attacks.
6. **Single-File Backup**:
   The entire database is `tithe_storehouse.db`. Backing up the church data is as simple as:
   ```bash
   cp tithe_storehouse.db tithe_storehouse_backup_$(date +%F).db
   ```
