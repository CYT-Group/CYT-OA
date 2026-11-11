"""纯转换单元测试；不连接 PG/MySQL，也不调用 AI。"""
import unittest
from datetime import datetime, timezone
from migrate_portal_hr_layout_jobs import local_time, transform


class HrLayoutMigrationTest(unittest.TestCase):
    def row(self):
        return dict(id=7, username="hr", status="succeeded", source_type="upload", source_name="制度.docx",
                    input_text="制度原文", input_chars=4, progress=100, stage="完成", summary="已分章",
                    blocks='[{"type":"text","text":"原文"}]', error=None,
                    created_at=datetime(2026, 6, 1, 0, tzinfo=timezone.utc),
                    finished_at=datetime(2026, 6, 1, 0, 1, tzinfo=timezone.utc))

    def test_success_preserves_identity_content_and_aware_time(self):
        row = self.row()
        result = transform(row, 99)
        self.assertEqual((result["id"], result["user_id"], result["actor_username"]), (7, 99, "hr"))
        self.assertEqual(result["blocks"], row["blocks"])
        self.assertEqual(result["created_at"], datetime(2026, 6, 1, 8))
        self.assertEqual(result["finished_at"], datetime(2026, 6, 1, 8, 1))

    def test_interrupted_task_is_not_replayed(self):
        for status in ("pending", "running"):
            row = self.row()
            row.update(status=status, blocks=None, finished_at=None, progress=20)
            result = transform(row, 99)
            self.assertEqual(result["status"], "failed")
            self.assertIn("未自动重放", result["error"])
            self.assertEqual(result["input_text"], row["input_text"])
            self.assertIsNone(result["blocks"])

    def test_naive_timestamp_is_not_guessed(self):
        with self.assertRaises(ValueError):
            local_time(datetime(2026, 6, 1))


if __name__ == "__main__":
    unittest.main()
