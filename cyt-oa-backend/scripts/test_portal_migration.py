"""迁移转换回归测试；不连接任何数据库。"""
import unittest
from datetime import datetime

from migrate_portal_hr_documents import preserve_orphan_snapshots
from migrate_portal_materials_ai import converted


class MigrationTest(unittest.TestCase):
    def test_missing_documents_keep_original_snapshots_in_recycle_bin(self):
        snapshot = {"id": 9, "document_id": 90, "title": "旧文档", "version": "v2.0",
                    "content_html": "<p>历史正文原样</p>", "content_blocks": None,
                    "published_at": datetime(2026, 10, 1, 1, 2, 3)}
        source = {"portal_hr_documents": [{"id": 1, "title": "仍在使用的文档"}],
                  "portal_hr_document_versions": [snapshot], "portal_hr_document_draft_versions": [],
                  "portal_hr_document_aliases": [], "portal_hr_document_entry_bindings": []}
        migrated, missing = preserve_orphan_snapshots(source)
        self.assertEqual([90], missing)
        recovered = migrated["portal_hr_documents"][1]
        self.assertEqual(90, recovered["id"])
        self.assertEqual("draft", recovered["status"])
        self.assertIsNotNone(recovered["deleted_at"])
        self.assertIsNone(recovered["slug"])
        self.assertEqual(snapshot["content_html"], recovered["content_html"])
        self.assertEqual(1, len(source["portal_hr_documents"]))
        self.assertEqual(snapshot, migrated["portal_hr_document_versions"][0])

    def test_archive_time_is_utc_even_after_database_clock_cutover(self):
        original = datetime(2026, 10, 2, 2, 3, 4, 123456)
        row = converted("portal_materials", {"archived_at": original, "created_at": original},
                        [("archived_at", "datetime"), ("created_at", "datetime")])
        self.assertEqual(10, row["archived_at"].hour)
        self.assertEqual(123456, row["archived_at"].microsecond)
        self.assertEqual(original, row["created_at"])


if __name__ == "__main__":
    unittest.main()
