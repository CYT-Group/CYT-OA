"""B1 身份与权限地基迁移：Portal Postgres → OA MySQL。

用法：
    python scripts/migrate_portal_identity.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_identity.py             # 真正写入

凭据不写进本文件：MySQL 读 application.yml/application-local.yml，PG 读 CYT-Portal 的 .env。
口径出处：docs/Portal管理面迁移契约.md §一「身份与权限」。
"""

import argparse
import json
import os
import re
import sys

import asyncpg
import pymysql

import portal_time as pt

API_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PORTAL_BACKEND = os.environ.get("CYT_PORTAL_BACKEND", r"D:/code/CYT-Portal/backend")
SYNTH_BASE = 900000  # dry-run 下给新行编的假 id，只在内存里存在，不会落库

# Portal 的能力位原样当权限串用：Portal 后端 require_capability(...) 判的就是这个名字，
# SSO 换到的 OA token 里带同一批字符串才能直接复用那套能力矩阵，改名等于两头都要改。
CAPABILITY_LABELS = {
    "can_enter_admin": "进入管理中心",
    "can_manage_users": "管理用户",
    "can_manage_roles": "管理角色权限",
    "can_manage_directory": "管理通讯录",
    "can_import": "批量导入",
    "can_create_content": "创建内容",
    "can_edit_any": "编辑任意内容",
    "can_review": "审核内容",
    "can_delete": "删除内容",
    "can_view_any_status": "查看任意状态内容",
    "can_view_own_draft": "查看本人草稿",
    "can_moderate_comments": "管理评论",
    "can_edit_hr_documents": "编辑 HR 文档",
    "can_view_hr_sop": "查看 HR SOP",
    "can_view_signoff_stats": "查看签收统计",
    "can_manage_training": "管理培训",
    "can_manage_events": "管理展会活动",
    "can_manage_products": "管理新品",
    "can_manage_brand_docs": "管理品牌手册",
    "can_manage_materials": "管理物品台账",
    "can_approve_any_material_apply": "审批任意物品申请",
    "can_configure_site_owners": "配置站点负责人",
    "can_manage_resumes": "管理简历识别",
    "can_manage_celebrations": "管理庆祝日历",
    "can_generate_contracts": "生成劳动合同",
    "can_send_messages": "发送消息",
    "can_manage_esports": "管理电竞室",
    "can_view_all_esports": "查看全部电竞预约",
    "can_view_all_reports": "查看全部汇报",
    "can_view_dashboards": "查看数据看板",
    "can_view_api_costs": "查看 API 成本",
    "can_manage_ai_settings": "管理 AI 配置",
    "can_manage_ai_keys": "管理 AI 密钥",
}


def _resolve(value):
    """占位符可能嵌在 JDBC 串中间（jdbc:mysql://${HOST:x}:${PORT:y}/db），逐个替换。"""
    def sub(m):
        return os.environ.get(m.group(1)) or (m.group(2) or "")
    return re.sub(r"\$\{([^:}]+)(?::([^}]*))?\}", sub, value).strip()


def read_mysql_conf():
    conf = {}
    for name in ("application.yml", "application-local.yml"):
        path = os.path.join(API_ROOT, "src", "main", "resources", name)
        if not os.path.exists(path):
            continue
        text = open(path, encoding="utf-8").read()
        for key, pattern in (
            ("url", r"url:\s*(jdbc:mysql://\S+)"),
            ("username", r"username:\s*(\S+)"),
            ("password", r"password:\s*(\S+)"),
        ):
            m = re.search(pattern, text, re.M)
            if m:
                conf[key] = _resolve(m.group(1))  # 后读的 application-local.yml 覆盖 application.yml
    jdbc = conf["url"].split("?")[0]
    host_port, db = jdbc.replace("jdbc:mysql://", "").rsplit("/", 1)
    host, _, port = host_port.partition(":")
    return {
        "host": host or "127.0.0.1",
        "port": int(port or 3306),
        "user": conf.get("username", "root"),
        "password": conf.get("password", ""),
        "database": db,
        "charset": "utf8mb4",
    }


def read_pg_dsn():
    env = open(os.path.join(PORTAL_BACKEND, ".env"), encoding="utf-8").read()
    url = re.search(r"^DATABASE_URL=(.+)$", env, re.M).group(1).strip()
    return url.replace("postgresql+asyncpg://", "postgresql://")


def dept_path(depts, dept_id, seen=None):
    """Portal 只给 parent_id，OA 的 sys_dept.path 是 /1/100/ 形式的全路径，按父链现算。"""
    seen = seen or set()
    if dept_id in seen or dept_id not in depts:
        return "/"
    seen.add(dept_id)
    parent = depts[dept_id]["parent_id"]
    if not parent or parent == dept_id:
        return "/%d/" % dept_id
    return dept_path(depts, parent, seen)[:-1] + "/%d/" % dept_id


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    depts = await pg.fetch("select id, name, parent_id, sort_order, leader_userids, is_active from portal_departments")
    users = await pg.fetch(
        "select id, username, display_name, role, department_ids, email, phone, avatar_url, is_active,"
        " birthday, hire_date, celebration_opt_out from portal_users order by id"
    )
    roles = await pg.fetch("select id, key, name, description, is_system from portal_roles order by id")
    grants = await pg.fetch("select role_key, permission_key from portal_role_permissions")
    audits = await pg.fetch(
        "select actor, action, target_table, target_id, before_value, after_value, comment, ip, trace_id, created_at"
        " from portal_admin_audit order by id"
    )
    # portal_employee_sync_jobs 的主键是 PGUUID、状态里带租约，是「上一次扫描跑到哪儿」的运行时态，
    # 历史 2 行没有迁移价值，只建表不搬数据（口径见 docs/Portal管理面迁移契约.md §一）。
    await pg.close()
    return depts, users, roles, grants, audits


def migrate(dept_rows, user_rows, role_rows, grant_rows, audit_rows, dry_run):
    my = pymysql.connect(**read_mysql_conf())
    cur = my.cursor()
    report = {}

    depts = {r["id"]: dict(r) for r in dept_rows}

    # ── 部门 ──
    dept_written = 0
    for r in dept_rows:
        leaders = r["leader_userids"]
        if isinstance(leaders, str):
            leaders = json.loads(leaders)
        leaders = [x for x in (leaders or []) if x]
        payload = (
            r["id"], r["parent_id"] or 0, r["name"], dept_path(depts, r["id"]),
            int(r["sort_order"] or 0), leaders[0] if leaders else "",
            json.dumps(leaders, ensure_ascii=False) if leaders else None,
            1 if r["is_active"] else 0,
        )
        if not dry_run:
            cur.execute(
                "INSERT INTO sys_dept (id, parent_id, name, path, sort_order, leader_userid, leader_userids, active)"
                " VALUES (%s,%s,%s,%s,%s,%s,%s,%s)"
                " ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), name=VALUES(name), path=VALUES(path),"
                " sort_order=VALUES(sort_order), leader_userid=VALUES(leader_userid),"
                " leader_userids=VALUES(leader_userids), active=VALUES(active)",
                payload,
            )
        dept_written += 1
    report["sys_dept"] = dept_written

    # ── 角色：Portal 的角色键补进 sys_role，已有的 admin/hr 不覆盖 ──
    role_ids = {}
    cur.execute("select id, code from sys_role")
    for rid, code in cur.fetchall():
        role_ids[code] = rid
    roles_new = 0
    for r in role_rows:
        if r["key"] in role_ids:
            continue
        if not dry_run:
            cur.execute(
                "INSERT INTO sys_role (code, name, built_in, remark) VALUES (%s,%s,0,%s)",
                (r["key"], r["name"], r["description"] or ""),
            )
            role_ids[r["key"]] = cur.lastrowid
        else:
            # dry-run 拿不到 lastrowid，给个合成 id，否则下游的关联计数会全部落空
            role_ids[r["key"]] = SYNTH_BASE + roles_new
        roles_new += 1
    report["sys_role 新增"] = roles_new

    # ── 能力位 → 按钮权限；权限矩阵 → sys_role_menu ──
    cur.execute("select id, permission from sys_menu")
    menu_by_perm = {p: i for i, p in cur.fetchall()}
    next_menu_id = max(menu_by_perm.values()) + 1 if menu_by_perm else 1
    new_menus = 0
    for key in sorted({g["permission_key"] for g in grant_rows}):
        if key in menu_by_perm:
            continue
        menu_by_perm[key] = next_menu_id
        if not dry_run:
            cur.execute(
                "INSERT INTO sys_menu (id, parent_id, name, type, path, permission, sort_order, visible)"
                " VALUES (%s,0,%s,'button','',%s,%s,1)",
                (next_menu_id, CAPABILITY_LABELS.get(key, key), key, next_menu_id),
            )
        next_menu_id += 1
        new_menus += 1
    report["sys_menu 新增能力位"] = new_menus

    cur.execute("select role_id, menu_id from sys_role_menu")
    existing_grants = {(a, b) for a, b in cur.fetchall()}
    added_grants = 0
    for g in grant_rows:
        role_id = role_ids.get(g["role_key"])
        menu_id = menu_by_perm.get(g["permission_key"])
        if not role_id or role_id < 0 or not menu_id or (role_id, menu_id) in existing_grants:
            continue
        if not dry_run:
            cur.execute("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (%s,%s)", (role_id, menu_id))
        existing_grants.add((role_id, menu_id))
        added_grants += 1
    report["sys_role_menu 新增"] = added_grants

    # ── 用户：Portal 的 username 就是企微 userid，所以要同时按 username 和 wecom_userid 认人，
    #    否则本地 admin（username=admin / wecom_userid=zhangwei）会和企微同名账号撞出两条记录。
    cur.execute("select id, username, wecom_userid from sys_user")
    user_ids = {}
    by_wecom = {}
    for i, u, w in cur.fetchall():
        user_ids[u] = i
        if w:
            by_wecom[w] = i
    portal_id_to_sys = {}
    users_new = users_upd = 0
    for r in user_rows:
        dept_ids = r["department_ids"]
        if isinstance(dept_ids, str):
            dept_ids = json.loads(dept_ids)
        main_dept = dept_ids[0] if dept_ids else None
        name = r["display_name"] or r["username"]
        sid = user_ids.get(r["username"]) or by_wecom.get(r["username"])
        if sid:
            if not dry_run:
                cur.execute(
                    "UPDATE sys_user SET name=%s, main_dept_id=%s, mobile=%s, email=%s, avatar=%s, enabled=%s,"
                    " wecom_userid=COALESCE(wecom_userid,%s) WHERE id=%s",
                    (name, main_dept, r["phone"] or "", r["email"] or "", r["avatar_url"] or "",
                     1 if r["is_active"] else 0, r["username"], sid),
                )
            users_upd += 1
        else:
            if not dry_run:
                cur.execute(
                    "INSERT INTO sys_user (wecom_userid, username, name, position, main_dept_id, mobile, email,"
                    " avatar, enabled) VALUES (%s,%s,%s,'',%s,%s,%s,%s,%s)",
                    (r["username"], r["username"], name, main_dept, r["phone"] or "", r["email"] or "",
                     r["avatar_url"] or "", 1 if r["is_active"] else 0),
                )
                user_ids[r["username"]] = cur.lastrowid
            else:
                user_ids[r["username"]] = SYNTH_BASE + users_new
            sid = user_ids[r["username"]]
            users_new += 1
        user_ids[r["username"]] = sid
        if sid > 0:
            portal_id_to_sys[r["id"]] = sid
        if sid > 0 and dept_ids and not dry_run:
            for did in dept_ids:
                cur.execute(
                    "INSERT INTO sys_user_dept (user_id, dept_id) VALUES (%s,%s)"
                    " ON DUPLICATE KEY UPDATE dept_id=VALUES(dept_id)",
                    (sid, did),
                )
        if sid > 0 and r["role"] and not dry_run:
            rid = role_ids.get(r["role"])
            if rid and rid > 0:
                cur.execute(
                    "INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (%s,%s)", (sid, rid)
                )
    report["sys_user 新增/已存在"] = "%d/%d" % (users_new, users_upd)

    # ── 档案：168 份生日与入职日期只存在于 Portal，落到 hrm_employee ──
    cur.execute("select id, user_id from hrm_employee where deleted = 0")
    emp_by_user = {u: i for i, u in cur.fetchall() if u}
    emp_new = emp_upd = 0
    for r in user_rows:
        sid = portal_id_to_sys.get(r["id"])
        if not sid:
            continue
        dept_ids = r["department_ids"]
        if isinstance(dept_ids, str):
            dept_ids = json.loads(dept_ids)
        row = {
            "name": r["display_name"] or r["username"], "user_id": sid, "mobile": r["phone"] or "",
            "email": r["email"] or "", "birthday": r["birthday"], "entry_time": r["hire_date"],
            "dept_id": dept_ids[0] if dept_ids else None,
            "celebration_opt_out": 1 if r["celebration_opt_out"] else 0,
            "entry_status": 1 if r["is_active"] else 4,
        }
        if sid in emp_by_user:
            if not dry_run:
                cols = [k for k in row if k != "user_id"]
                cur.execute(
                    "UPDATE hrm_employee SET %s WHERE id=%%s" % ", ".join("%s=%%s" % c for c in cols),
                    tuple(row[c] for c in cols) + (emp_by_user[sid],),
                )
            emp_upd += 1
        else:
            if not dry_run:
                cur.execute(
                    "INSERT INTO hrm_employee (%s) VALUES (%s)" % (", ".join(row), ", ".join(["%s"] * len(row))),
                    tuple(row.values()),
                )
                emp_by_user[sid] = cur.lastrowid
            emp_new += 1
    report["hrm_employee 新增/更新"] = "%d/%d" % (emp_new, emp_upd)
    report["其中有生日或入职日期"] = sum(1 for r in user_rows if r["birthday"] or r["hire_date"])

    # ── 操作日志 ──
    cur.execute("select id, username from sys_user")
    uid_by_username = {u: i for i, u in cur.fetchall()}
    logs = 0
    cutover_rows = 0
    for a in audit_rows:
        occurred_at = pt.server_clock(a["created_at"])
        cutover_rows += pt.is_cutover_day(a["created_at"])
        if not dry_run:
            cur.execute(
                "INSERT INTO sys_operate_log (trace_id, user_id, user_type, type, sub_type, biz_id, action,"
                " target_table, before_value, after_value, comment, user_ip, created_at)"
                " VALUES (%s,%s,2,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                (
                    a["trace_id"] or "", uid_by_username.get(a["actor"]), "后台管理", a["action"] or "",
                    _biz_id(a["target_id"]), "", a["target_table"] or "", a["before_value"], a["after_value"],
                    a["comment"] or "", a["ip"] or "", occurred_at,
                ),
            )
        logs += 1
    report["sys_operate_log"] = logs
    # 不写 created_at 的话 MySQL 会用 CURRENT_TIMESTAMP 兜底，364 行历史日志全变成「迁移执行的那一刻」
    report["审计时间落在时钟切换当天"] = cutover_rows

    try:
        if dry_run:
            my.rollback()
        else:
            my.commit()
    except Exception:
        my.rollback()  # 半途失败不留半套身份数据
        raise
    finally:
        my.close()
    return report


def _biz_id(raw):
    """Portal 的 target_id 是文本列，可能是数字也可能是 slug，取数字前缀当业务编号。"""
    if raw is None:
        return None
    m = re.search(r"\d+", str(raw))
    return int(m.group()) if m else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="只统计将要写入的行数，事务回滚")
    args = ap.parse_args()
    rows = asyncio_run(fetch_portal())
    report = migrate(*rows, dry_run=args.dry_run)
    print("\n%s 迁移结果" % ("[dry-run 未写库] " if args.dry_run else ""))
    for k, v in report.items():
        print("  %-24s %s" % (k, v))


def asyncio_run(coro):
    import asyncio
    return asyncio.run(coro)


if __name__ == "__main__":
    sys.exit(main())
