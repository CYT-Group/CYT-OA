from pathlib import Path
import re
import unittest


class NginxDocumentationRoutesTest(unittest.TestCase):
    def test_documentation_roots_and_nested_assets_reach_the_backend(self):
        config = (Path(__file__).parents[1] / 'nginx.conf').read_text(encoding='utf-8')
        pattern = re.search(r'location\s+~\s+(\^/\S+)\s+\{', config).group(1)
        for path in ['/v3/api-docs', '/v3/api-docs/', '/v3/api-docs/swagger-config',
                     '/swagger-ui.html', '/swagger-ui/index.html', '/swagger-resources/', '/webjars/example.js']:
            with self.subTest(path=path):
                self.assertIsNotNone(re.search(pattern, path))
        for path in ['/me/overview', '/portal', '/v3/api-docs-other', '/swagger-ui.html-other']:
            with self.subTest(path=path):
                self.assertIsNone(re.search(pattern, path))


if __name__ == '__main__':
    unittest.main()
