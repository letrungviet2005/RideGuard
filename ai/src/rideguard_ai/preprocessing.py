"""Assign whole sessions before any windowing to avoid session leakage."""
from hashlib import sha256


def session_split(session_id: str) -> str:
    """Stable 80/10/10 assignment; never split a session's frames individually."""
    if not session_id.strip():
        raise ValueError("session_id must not be blank")
    bucket = int.from_bytes(sha256(session_id.encode("utf-8")).digest()[:8], "big") % 100
    return "train" if bucket < 80 else "validation" if bucket < 90 else "test"
