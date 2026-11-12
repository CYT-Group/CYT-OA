#!/usr/bin/env python3
"""通过 Flyway 管理 OA 部署迁移；不在应用启动时自动修改数据库。"""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import re
import subprocess


FLYWAY_IMAGE = 'flyway/flyway:11.20.2-alpine'
MIGRATION_PATH = Path('cyt-oa-backend/src/main/resources/db/postgresql/migration')
NAME = re.compile(r'V(\d{8})_(\d{3})__([a-z][a-z0-9_]*)\.sql\Z')


def check_migrations(directory):
    if not directory.is_dir():
        raise ValueError('缺少 SQL 迁移目录：' + str(directory))
    files = sorted(path for path in directory.rglob('*') if path.is_file() and path.suffix.lower() == '.sql')
    if not files:
        raise ValueError('迁移目录中没有版本 SQL')
    versions = set()
    for path in files:
        match = NAME.fullmatch(path.name)
        if path.parent != directory or not match:
            raise ValueError('SQL 必须直接放在迁移目录，命名 VYYYYMMDD_NNN__英文说明.sql：' + path.name)
        datetime.strptime(match[1], '%Y%m%d')
        version = (int(match[1]), int(match[2]))
        if version[1] == 0 or version in versions:
            raise ValueError('迁移版本重复或序号为 000：' + path.name)
        versions.add(version)
        content = path.read_bytes()
        if content.startswith(b'\xef\xbb\xbf'):
            raise ValueError('SQL 使用 UTF-8 无 BOM：' + path.name)
        text = content.decode('utf-8')
        if not text.strip():
            raise ValueError('迁移 SQL 不能为空：' + path.name)
    return files


def database_environment(container):
    result = subprocess.run(['docker', 'inspect', container], check=True,
                            capture_output=True, text=True)
    info = json.loads(result.stdout)[0]
    if not info['State']['Running']:
        raise ValueError('OA 数据库容器未运行')
    values = dict(item.split('=', 1) for item in info['Config']['Env'] if '=' in item)
    for key in ('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD'):
        if not values.get(key):
            raise ValueError('数据库容器缺少配置：' + key)
    database = values['POSTGRES_DB']
    if not re.fullmatch(r'[A-Za-z0-9_]+', database):
        raise ValueError('数据库名称只允许字母、数字和下划线')
    return {
        'FLYWAY_URL': 'jdbc:postgresql://127.0.0.1:5432/' + database,
        'FLYWAY_USER': values['POSTGRES_USER'],
        'FLYWAY_PASSWORD': values['POSTGRES_PASSWORD'],
    }


def flyway_command(container, directory, image, command):
    args = ['docker', 'run', '--rm', '--network', 'container:' + container,
            '--volume', str(directory.resolve()) + ':/flyway/sql:ro']
    for key in ('FLYWAY_URL', 'FLYWAY_USER', 'FLYWAY_PASSWORD'):
        # 只传环境变量名，真实口令不进入命令行或日志。
        args.extend(['--env', key])
    args.extend([image, '-locations=filesystem:/flyway/sql', '-encoding=UTF-8',
                 '-cleanDisabled=true', '-baselineOnMigrate=false',
                 '-validateOnMigrate=true', '-validateMigrationNaming=true',
                 '-failOnMissingLocations=true', '-outOfOrder=false',
                 '-ignoreMigrationPatterns=', '-placeholderReplacement=false',
                 '-connectRetries=10'])
    args.append(command)
    return args


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--migrations-dir', type=Path)
    parser.add_argument('--database-container', default='cyt-oa-db')
    parser.add_argument('command', nargs='?', default='migrate',
                        choices=('migrate', 'info', 'validate', 'lint'))
    args = parser.parse_args()
    directory = args.migrations_dir or args.root / MIGRATION_PATH
    files = check_migrations(directory)
    print('==> SQL 迁移目录：' + str(directory) + '；版本文件：' + str(len(files)), flush=True)
    if args.command == 'lint':
        return
    environment = dict(os.environ, **database_environment(args.database_container))
    image = os.environ.get('OA_FLYWAY_IMAGE', FLYWAY_IMAGE)
    subprocess.run(flyway_command(args.database_container, directory, image, args.command),
                   env=environment, check=True)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, subprocess.CalledProcessError) as error:
        raise SystemExit('OA 数据库迁移失败：' + str(error)) from None
