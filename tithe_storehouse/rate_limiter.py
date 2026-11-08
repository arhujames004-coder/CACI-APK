import time
from collections import defaultdict
from threading import Lock

class RateLimiter:
    """
    Sliding window in-memory rate limiter per key (IP or phone number).
    Thread-safe and lightweight for Termux and lightweight servers.
    """
    def __init__(self, limit=60, window_seconds=60):
        self.limit = limit
        self.window_seconds = window_seconds
        self.requests = defaultdict(list)
        self.lock = Lock()

    def is_allowed(self, key):
        now = time.time()
        with self.lock:
            # Filter out requests older than window
            cutoff = now - self.window_seconds
            timestamps = [t for t in self.requests[key] if t > cutoff]
            if len(timestamps) >= self.limit:
                self.requests[key] = timestamps
                return False
            timestamps.append(now)
            self.requests[key] = timestamps
            return True

    def reset(self, key=None):
        with self.lock:
            if key:
                self.requests.pop(key, None)
            else:
                self.requests.clear()

# Global instances for USSD and Webhook endpoints
ussd_limiter = RateLimiter(limit=60, window_seconds=60)
webhook_limiter = RateLimiter(limit=120, window_seconds=60)
admin_limiter = RateLimiter(limit=30, window_seconds=60)
