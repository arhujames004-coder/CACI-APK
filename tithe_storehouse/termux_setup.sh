#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Tithe Storehouse - Termux Setup & Launch Script
# Run this directly in Termux on Android:
#   pkg update && pkg install git -y
#   bash tithe_storehouse/termux_setup.sh
# ==============================================================================

set -e

echo "=========================================================="
echo "   Tithe Storehouse - Church Treasury System for Termux   "
echo "=========================================================="

echo "[1/5] Updating Termux packages..."
pkg update -y
pkg install -y python python-pip sqlite openssl clang libffi

echo "[2/5] Setting up Python virtual environment..."
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
source venv/bin/activate

echo "[3/5] Installing dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

echo "[4/5] Checking environment configuration..."
if [ ! -f ".env" ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo "Created .env with default test credentials."
fi

echo "[5/5] Initializing SQLite database and audit logs..."
python3 -c "
import sys
sys.path.insert(0, '.')
from tithe_storehouse.database import init_db
init_db(seed=True)
print('Database verified with append-only audit triggers.')
"

echo "=========================================================="
echo "Setup complete! Starting Tithe Storehouse server..."
echo "Admin Dashboard: http://127.0.0.1:5000"
echo "Default Login: admin / GraceTithe2026!"
echo "Press Ctrl+C to stop."
echo "=========================================================="

python3 tithe_storehouse/run.py
