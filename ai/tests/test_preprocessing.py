import unittest
from rideguard_ai.preprocessing import session_split


class SessionSplitTest(unittest.TestCase):
    def test_frames_from_session_never_leak(self):
        frames = [(f"session-{session}", frame) for session in range(100) for frame in range(10)]
        assignments = {}
        for session, _ in frames:
            assignments.setdefault(session, set()).add(session_split(session))
        self.assertTrue(all(len(splits) == 1 for splits in assignments.values()))
        self.assertEqual({session_split(s) for s in assignments}, {"train", "validation", "test"})

    def test_blank_session_is_rejected(self):
        with self.assertRaises(ValueError):
            session_split("  ")

    def test_assignment_is_stable(self):
        self.assertEqual(session_split("session-1"), "train")
