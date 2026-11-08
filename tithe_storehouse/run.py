#!/usr/bin/env python3
import sys
import os

# Ensure package root is in path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tithe_storehouse.app import create_app
from tithe_storehouse.config import Config

if __name__ == "__main__":
    print(f"Starting Tithe Storehouse Server for {Config.CHURCH_NAME}...")
    print(f"Admin Dashboard: http://{Config.SERVER_HOST}:{Config.SERVER_PORT}/")
    print(f"Africa's Talking USSD URL: http://{Config.SERVER_HOST}:{Config.SERVER_PORT}/ussd")
    print(f"Paystack Webhook URL: http://{Config.SERVER_HOST}:{Config.SERVER_PORT}/webhook/paystack")
    app = create_app()
    app.run(host=Config.SERVER_HOST, port=Config.SERVER_PORT, debug=False)
