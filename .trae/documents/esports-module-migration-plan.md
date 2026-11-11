# 电竞室 esports 模块迁移计划

## Context（为什么做这件事）

Portal（FastAPI+PG）的「电竞室」是一个**设置驱动的多级审批预约系统**，非纯 CRUD。
按迁移契约，数据处理与后台管理全迁入 OA（Java/Spring Boot/MySQL），门户只保留静态展示、直连 OA Open API。

本切片把 Portal 的 `/esports/*` 整组端点迁到 OA `module/portal/esports`，含 4 张表、领域规则（多级审批状态机 + 独占时段冲突检测 + 上海营业时间窗）、以及门户读侧切流。

**时间换算结论（重要，覆盖此前 handoff 的「server_clock」猜测）**：
Portal 的 `application/esports.py` 所有时间列都用 `datetime.utcnow()` 显式写入
（create_booking L191/197、act_on_booking L263-279、add_log L116、update_settings L79），
因此 esports 的 **created_at / updated_at / acted_at / reviewed_at / cancelled_at / logs.created_at / settings.updated_at 一律走 `app_utc`（恒 +8h）**，不走 server_clock。

**通知（企微推送）挂起**：`messages.send_message(wecom_sync=True)` 依赖 OA 尚未就绪的 WecomGatewayClient，本切片不接通知，返回体不含 `notification` 字段（契约已记录，随推送切片落地）。

---

## 落点与端点设计

全部端点放在 **单一 Open 控制器** `controller/app/esports/PortalEsportsOpenController`，路径 `/api/portal/esports/*`。
理由：电竞室是全员自助预约功能（Portal 只有一个 `/esports` router），管理/审批靠**服务层能力判定**（对齐契约「可见性按能力收敛」），不做 `/admin-api` 分叉，也不在本切片建 OA 控制台管理页。

| 方法 | 路径 | 能力判定（服务层） |
|---|---|---|
| GET | `/settings` | 登录即可；返回里带 can_manage/can_view_all/can_review |
| PUT | `/settings` | `can_manage_esports` |
| GET | `/approvers` | `can_manage_esports` |
| GET | `/participants` | 登录即可 |
| GET | `/bookings?date_from&date_to&scope&status&q` | scope=all 需 `can_view_all_esports` |
| POST | `/bookings` | 登录即可 |
| GET | `/bookings/{id}` | 参与人可看全量；非参与人只看占位 |
| POST | `/bookings/{id}/approve` | `can_review_esports(user, 当前环节审批人)` |
| POST | `/bookings/{id}/reject` | 同上（remark 必填） |
| POST | `/bookings/{id}/cancel` | 本人或 `can_manage_esports` |

能力位新增到 `PortalCapability`：`MANAGE_ESPORTS = "can_manage_esports"`、`VIEW_ALL_ESPORTS = "can_view_all_esports"`。
`can_review_esports` = `can_manage_esports || user.id ∈ 当前待审环节审批人`（服务层内联，不复用矩阵）。

---

## 新建文件

### 1. 建表 `src/main/resources/db/portal-esports-schema.sql`
4 张表，PG→MySQL 等价改写（JSONB→JSON、boolean→TINYINT(1)、CHECK 保留、id 保留、FK 只落在 portal_* 内部表之间，user_id 系列只存值不建 FK，对齐 signoff/training 口径）：
- `portal_esports_settings`：单行 id=1（无自增，CHECK id=1）；`open_weekdays JSON`、`approval_stages JSON`；`approver_user_id` 存值。
- `portal_esports_bookings`：`booking_date DATE`、`start_time/end_time TIME`、`participants JSON NULL`、`current_stage_index INT NULL`；索引 `(booking_date,status)`、`(applicant_user_id,booking_date)`、`(approver_user_id,booking_date)`。
- `portal_esports_approval_steps`：`UNIQUE(booking_id,stage_index,approver_user_id)`、FK→bookings ON DELETE CASCADE、索引 `(approver_user_id,status,booking_id)`。
- `portal_esports_booking_logs`：FK→bookings ON DELETE CASCADE、索引 `(booking_id,created_at,id)`。

CHECK 保留：settings(singleton/time/advance/duration/approval_mode)、bookings(time/participants/status/approval_mode/current_stage)、steps(stage/status)、logs(operation)。

### 2. 迁移脚本 `scripts/migrate_portal_esports.py`
复用 `read_pg_dsn`/`read_mysql_conf`（migrate_portal_identity）+ `portal_time.py`。4 表按依赖顺序（settings → bookings → steps → logs）。行 id 原样保留、已存在跳过、500 行/批、`--dry-run`。
- JSONB（open_weekdays/approval_stages/participants）：asyncpg 读回 list/dict，`json.dumps(ensure_ascii=False)` 后写 MySQL JSON 列（复用 training 脚本 `_pages` 手法）。
- 时间列统一 `pt.app_utc()`（恒 +8h）；`time`/`date` 直接透传（pymysql 处理 datetime.time/date）。

### 3. DO（`dal/dataobject/esports/`，4 个）
`@TableName(value=..., autoResultMap=true)`；JSON 列 `@TableField(typeHandler = JacksonTypeHandler.class)`；可清空列 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`（participants、purpose、review_remark、current_stage_index 等）。字段类型：`LocalDate bookingDate`、`LocalTime startTime/endTime`、`LocalDateTime` 各时间戳、`List<Integer> openWeekdays`、`List<Map<String,Object>> approvalStages/participants`。

### 4. Mapper（`dal/mysql/esports/`，4 个，extends BaseMapperX）
- Settings：`selectById(1)`；`@Select("... FOR UPDATE")` 锁定单行。
- Booking：`@Select` 的 `selectByIdForUpdate`；overlap 半开区间查询（`start_time < ? AND end_time > ?` + `status IN ('pending','approved')` + `booking_date = ?` + 排除自身）；列表过滤（scope/status/q 字面转义 LIKE）。
- Step：按 booking_id 批量查 + `FOR UPDATE`，按 `(booking_id,stage_index,id)` 排序。
- Log：按 booking_id 批量查，按 id 排序。

### 5. 领域规则 `domain/esports/EsportsRules.java`
纯静态函数，1:1 对应 `domain/esports.py`：`validateSettings`/`settingsPlan`/`requestedPlan`/`validatePlan`/`pendingSteps`/`transitionApproval`/`validateBooking`/`participantCountFor`/`actionTimeOpen`/`validateTransition`；常量 `STATUSES`/`OCCUPYING_STATUSES`；`shanghaiNow()` 用 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))`。

### 6. Service/Impl（`service/esports/`）
`PortalEsportsService` + `PortalEsportsServiceImpl`（@Transactional，锁序 settings→booking→steps，对齐 Portal）：
- `readSettings` / `updateSettings` / `listApprovers` / `listParticipants` / `listBookings` / `bookingDetail` / `createBooking` / `actOnBooking`。
- 能力判定用 `PortalAccess.has(userId, capability)`；当前人用 `PortalOperatorResolver.resolve`；用户解析用 `AdminUserApi.getUserList(ids)`/`getUser(id)`/`getUserListByStatus(0)` + `DeptApi` 补部门名。
- 审批快照、legacy 无节点单合成（`effectiveSteps`）、环节推进、取消/审批时间窗、overlap 检测全部落地；通知留空。

### 7. Open 控制器 `controller/app/esports/PortalEsportsOpenController.java`
`@RequestMapping("/api/portal/esports")`，thin wire，`getLoginUserId()` 取当前人，调 service，`BeanUtils.toBean` 转 VO。

### 8. VO（`controller/app/esports/vo/`）
- Req：`EsportsSettingsUpdateReqVO`（含 `StageReqVO.approverUserIds`）、`EsportsBookingCreateReqVO`、`EsportsActionReqVO`、`EsportsRejectReqVO`。
- Resp：`EsportsSettingsRespVO`、`EsportsBookingRespVO`（含 approvalSteps/logs/isMine/canReview/canCancel，private 字段按 private 收敛）、`EsportsUserRespVO`（id/username/displayName/department，供 approvers/participants）。
- `LocalTime` 字段加 `@JsonFormat(pattern="HH:mm")`、`LocalDate` 走 ISO（`yyyy-MM-dd`）。

---

## 修改文件

1. `domain/PortalCapability.java`：新增 `MANAGE_ESPORTS`、`VIEW_ALL_ESPORTS`。
2. `enums/ErrorCodeConstants.java`：新增 `1-060-920-xxx` 段（5 个码）：
   - `920_000 ESPORTS_BOOKING_NOT_FOUND`(404)
   - `920_001 ESPORTS_FORBIDDEN`(403,"{}")
   - `920_002 ESPORTS_SETTINGS_INVALID`(422,"{}")
   - `920_003 ESPORTS_BOOKING_INVALID`(422,"{}")
   - `920_004 ESPORTS_CONFLICT`(409,"{}")
3. `D:\code\CYT-Portal\frontend\src\api\oa.ts`：`CODE_TO_STATUS` 追加 `1-060-920` 5 行映射。
4. `D:\code\CYT-Portal\frontend\src\api\index.ts`：把 esports 的 API 函数改用 `oaGet/oaPost`（切流，具体函数名执行时按文件现状定位）。
5. `docs/Portal管理面迁移契约.md`：进度段落更新为 esports 已迁（表/脚本/Open API/门户切流），通知依赖企微推送挂起。

---

## 验证

1. 建表：在 `cyt_oa_center` 库执行 `portal-esports-schema.sql`。
2. 迁移：`python scripts/migrate_portal_esports.py --dry-run` 核对行数/时间换算/切天统计，再正式跑。
3. 编译：`D:\apache-maven-3.10.0\bin\mvn.cmd -q -DskipTests compile`（cwd=cyt-oa-backend；OOM 删 hs_err_pid*.log 重试）。
4. 门户前端类型检查：`npx vue-tsc --noEmit`（cwd=D:\code\CYT-Portal\frontend）。
5. 手工冒烟（可选）：Open API 读 settings/participants、建单→多级审批→取消，核对 overlap 冲突与状态机。

---

## 待澄清/风险（无需阻塞，已在计划内取舍）

- **时间换算**：按代码实锤走 `app_utc`；dry-run 报告切天计数，若与预期不符可复核。
- **列表 total**：OA `Result<List>` 不含 total，门户前端切流时按 `data.length` 或局部适配。
- **通知**：企微推送未就绪，本切片不接，随推送切片补。
