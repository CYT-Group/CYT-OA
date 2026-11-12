"""Portal Postgres 的时间列搬到 OA PostgreSQL 时的换算口径，B1（审计）与 B2（内容）共用。

Portal 侧一张表里混着两套写法：
  1) server_default=func.now() 的 created_at / updated_at / viewed_at / audit.created_at
     —— 跟着 PG 服务端时钟走，而 PG 的时钟在 2026-07-23 从 UTC 切到了 Asia/Shanghai；
  2) 应用层 datetime.utcnow() 写的 published_at / archived_at —— 恒为 UTC 裸时间，
     与时钟切换无关（application/approval.py:133、application/categories.py:225）。

所以 1 要按写入时刻分段，2 一律 +8h。判「写入时刻」只能用存量值本身当代理，好在切换前后
的值天然错开：切换前存的是 wall-UTC，切换后存的是 wall-UTC+8，落在 [切换, 切换+8h) 区间
的值根本不存在，实测 portal_page_views.viewed_at 在 2026-07-23 08:00~14:00 是空的，
取这个空档里任何一个时刻做阈值都等价。

OA 侧 PostgreSQL 会话时区固定为 Asia/Shanghai，Java 用 LocalDateTime.now()，
搬过来的值必须是北京时间才对得上。
"""

from datetime import datetime, timedelta, timezone

UTC_OFFSET = timedelta(hours=8)
SHANGHAI = timezone(UTC_OFFSET)

# 落在实测空档（07-23 07:0x → 15:0x）中间，前后各留 >4h 余量
UTC_ERA_CUTOVER = datetime(2026, 7, 23, 12, 0)

# 切换当天写的行：阈值估偏就会翻边，迁移报告里单独点出来给人过目
CUTOVER_DAY_LO = datetime(2026, 7, 23, 0, 0)
CUTOVER_DAY_HI = datetime(2026, 7, 24, 0, 0)


def server_clock(value):
    """func.now() 系列列：切换前是 UTC 裸时间，补 8 小时；切换后已是北京时间，原样搬。"""
    if value is None:
        return None
    if value.utcoffset() is not None:
        return value.astimezone(SHANGHAI).replace(tzinfo=None)
    return value + UTC_OFFSET if value < UTC_ERA_CUTOVER else value


def app_utc(value):
    """datetime.utcnow() 系列列：不分段，一律 +8h。"""
    if value is None:
        return None
    if value.utcoffset() is not None:
        return value.astimezone(SHANGHAI).replace(tzinfo=None)
    return value + UTC_OFFSET


def is_cutover_day(value):
    if value is not None and value.utcoffset() is not None:
        value = value.astimezone(SHANGHAI).replace(tzinfo=None)
    return value is not None and CUTOVER_DAY_LO <= value < CUTOVER_DAY_HI
