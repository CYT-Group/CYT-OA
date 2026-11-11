"""只验证纯时间/类型转换，不连接任何数据库。python scripts/test_portal_management_migration.py"""
import unittest
from datetime import datetime, timedelta
from migrate_portal_management import convert


class ManagementTimesTest(unittest.TestCase):
    def test_server_and_application_clock_are_not_conflated(self):
        value = datetime(2026, 11, 9, 3, 30)
        self.assertEqual(value, convert({"created_at": value}, ["created_at"], "portal_resumes")["created_at"])
        self.assertEqual(value + timedelta(hours=8), convert({"confirmed_at": value}, ["confirmed_at"], "portal_resumes")["confirmed_at"])
        self.assertEqual(value + timedelta(hours=8), convert({"created_at": value}, ["created_at"], "portal_resume_interview_processes")["created_at"])
        self.assertEqual(value + timedelta(hours=8), convert({"created_at": value}, ["created_at"], "portal_celebration_cards")["created_at"])

    def test_push_logs_keep_source_clock_and_failure_sentinel(self):
        value = datetime(2026, 11, 9, 3, 30)
        self.assertEqual(value + timedelta(hours=8), convert({"pushed_at": value, "trigger": "birthday_card"}, ["pushed_at"], "portal_wecom_push_log")["pushed_at"])
        self.assertEqual(value, convert({"pushed_at": value, "trigger": "news"}, ["pushed_at"], "portal_wecom_push_log")["pushed_at"])
        sentinel = datetime(1970, 1, 1)
        self.assertEqual(sentinel, convert({"pushed_at": sentinel, "trigger": "news"}, ["pushed_at"], "portal_wecom_push_log")["pushed_at"])


if __name__ == "__main__":
    unittest.main()
