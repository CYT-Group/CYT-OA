-- PostgreSQL 17+ complete empty-database schema.

-- JSON documents use TEXT with JSON validation so existing JDBC String writes remain valid.

-- SMALLINT stores 0/1 flags and numeric status codes used by the application.

SET TIME ZONE 'Asia/Shanghai';

-- Fixed UTF-8 canonical JSON encoding makes this fingerprint deterministic.
CREATE FUNCTION cyt_json_fingerprint(value TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE STRICT AS $$
  SELECT encode(sha256(convert_to(value::jsonb::text, 'UTF8')), 'hex')
$$;

-- Maintain update timestamps while preserving explicit values and ignoring no-op updates.
-- Stored generated columns are unavailable in BEFORE triggers; omit them from the comparison.
CREATE FUNCTION cyt_touch_updated_column() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (to_jsonb(NEW) - TG_ARGV[1:TG_NARGS]) IS DISTINCT FROM (to_jsonb(OLD) - TG_ARGV[1:TG_NARGS])
     AND (to_jsonb(NEW) -> TG_ARGV[0]) IS NOT DISTINCT FROM (to_jsonb(OLD) -> TG_ARGV[0]) THEN
    NEW := jsonb_populate_record(NEW, jsonb_build_object(TG_ARGV[0], LOCALTIMESTAMP));
  END IF;
  RETURN NEW;
END
$$;

CREATE TABLE flow_definition (
    id bigint NOT NULL,
    flow_code varchar(40) NOT NULL,
    flow_name varchar(100) NOT NULL,
    model_value varchar(40) NOT NULL DEFAULT 'CLASSICS',
    category varchar(100) DEFAULT NULL,
    version varchar(20) NOT NULL,
    is_publish SMALLINT NOT NULL DEFAULT '0',
    form_custom char(1) DEFAULT 'N',
    form_path varchar(100) DEFAULT NULL,
    activity_status SMALLINT NOT NULL DEFAULT '1',
    listener_type varchar(100) DEFAULT NULL,
    listener_path varchar(400) DEFAULT NULL,
    ext varchar(500) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_form (
    id bigint NOT NULL,
    form_code varchar(40) NOT NULL,
    form_name varchar(100) NOT NULL,
    version varchar(20) NOT NULL,
    is_publish SMALLINT NOT NULL DEFAULT '0',
    form_type SMALLINT DEFAULT '0',
    form_path varchar(100) DEFAULT NULL,
    form_content TEXT,
    ext varchar(400) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_his_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    task_id bigint NOT NULL,
    node_code varchar(100) DEFAULT NULL,
    node_name varchar(100) DEFAULT NULL,
    node_type SMALLINT DEFAULT NULL,
    target_node_code varchar(200) DEFAULT NULL,
    target_node_name varchar(200) DEFAULT NULL,
    approver varchar(40) DEFAULT NULL,
    cooperate_type SMALLINT NOT NULL DEFAULT '0',
    collaborator varchar(500) DEFAULT NULL,
    skip_type varchar(10) NOT NULL,
    flow_status varchar(20) NOT NULL,
    form_custom char(1) DEFAULT 'N',
    form_path varchar(100) DEFAULT NULL,
    message varchar(500) DEFAULT NULL,
    variable text,
    ext text,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_instance (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    business_id varchar(40) NOT NULL,
    node_type SMALLINT NOT NULL,
    node_code varchar(40) NOT NULL,
    node_name varchar(100) DEFAULT NULL,
    variable text,
    flow_status varchar(20) NOT NULL,
    activity_status SMALLINT NOT NULL DEFAULT '1',
    def_json text,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    ext varchar(500) DEFAULT NULL,
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_node (
    id bigint NOT NULL,
    node_type SMALLINT NOT NULL,
    definition_id bigint NOT NULL,
    node_code varchar(100) NOT NULL,
    node_name varchar(100) DEFAULT NULL,
    permission_flag varchar(200) DEFAULT NULL,
    node_ratio varchar(200) DEFAULT NULL,
    coordinate varchar(100) DEFAULT NULL,
    any_node_skip varchar(100) DEFAULT NULL,
    listener_type varchar(100) DEFAULT NULL,
    listener_path varchar(400) DEFAULT NULL,
    form_custom char(1) DEFAULT 'N',
    form_path varchar(100) DEFAULT NULL,
    version varchar(20) NOT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    ext text,
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_skip (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    now_node_code varchar(100) NOT NULL,
    now_node_type SMALLINT DEFAULT NULL,
    next_node_code varchar(100) NOT NULL,
    next_node_type SMALLINT DEFAULT NULL,
    skip_name varchar(100) DEFAULT NULL,
    skip_type varchar(40) DEFAULT NULL,
    skip_condition varchar(200) DEFAULT NULL,
    coordinate varchar(100) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    node_code varchar(100) NOT NULL,
    node_name varchar(100) DEFAULT NULL,
    node_type SMALLINT NOT NULL,
    flow_status varchar(20) NOT NULL,
    form_custom char(1) DEFAULT 'N',
    form_path varchar(100) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE flow_user (
    id bigint NOT NULL,
    type char(1) NOT NULL,
    processed_by varchar(80) DEFAULT NULL,
    associated bigint NOT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    create_by varchar(80) DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    update_by varchar(64) DEFAULT '',
    del_flag char(1) DEFAULT '0',
    tenant_id varchar(40) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_attendance_clock (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    clock_time TIMESTAMP DEFAULT NULL,
    type int DEFAULT NULL,
    attendance_time TIMESTAMP DEFAULT NULL,
    source_type int DEFAULT NULL,
    status int DEFAULT NULL,
    stage int DEFAULT NULL,
    address varchar(512) DEFAULT NULL,
    longitude decimal(10,6) DEFAULT NULL,
    latitude decimal(10,6) DEFAULT NULL,
    ssid varchar(128) DEFAULT NULL,
    mac varchar(512) DEFAULT NULL,
    remark text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_attendance_group (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    dept_ids text,
    employee_ids text,
    open_wifi_card SMALLINT DEFAULT NULL,
    open_point_card SMALLINT DEFAULT NULL,
    shifts text,
    rest SMALLINT DEFAULT NULL,
    special_dates text,
    points text,
    wifis text,
    deduct_rule text,
    default_status SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_attendance_holiday (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    date TIMESTAMP DEFAULT NULL,
    type int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_attendance_leave (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    type varchar(255) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    day decimal(10,4) DEFAULT NULL,
    reason varchar(255) DEFAULT NULL,
    remark text,
    approval_status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    approval_time TIMESTAMP DEFAULT NULL,
    approval_reason text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_config (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    type int DEFAULT NULL,
    value varchar(255) DEFAULT NULL,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    job_number varchar(64) DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    mobile varchar(32) DEFAULT NULL,
    country varchar(255) DEFAULT NULL,
    nation varchar(255) DEFAULT NULL,
    id_type int DEFAULT NULL,
    id_number varchar(64) DEFAULT NULL,
    sex int DEFAULT NULL,
    email varchar(128) DEFAULT NULL,
    native_place varchar(255) DEFAULT NULL,
    birthday TIMESTAMP DEFAULT NULL,
    age int DEFAULT NULL,
    address varchar(512) DEFAULT NULL,
    highest_education int DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    leader_employee_id bigint DEFAULT NULL,
    entry_status int DEFAULT NULL,
    status int DEFAULT NULL,
    type int DEFAULT NULL,
    entry_time TIMESTAMP DEFAULT NULL,
    probation int DEFAULT NULL,
    regular_time TIMESTAMP DEFAULT NULL,
    leave_time TIMESTAMP DEFAULT NULL,
    post_name varchar(128) DEFAULT NULL,
    post_level varchar(255) DEFAULT NULL,
    work_city varchar(255) DEFAULT NULL,
    work_address varchar(512) DEFAULT NULL,
    work_detail_address varchar(512) DEFAULT NULL,
    channel_id bigint DEFAULT NULL,
    company_age_start_time TIMESTAMP DEFAULT NULL,
    company_age int DEFAULT NULL,
    candidate_id bigint DEFAULT NULL,
    celebration_opt_out SMALLINT NOT NULL DEFAULT '0',
    remark text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    active_user_id bigint GENERATED ALWAYS AS ((case when (deleted = 0) then user_id else NULL end)) STORED,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_certificate (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    level varchar(255) DEFAULT NULL,
    no varchar(64) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    issuing_authority varchar(255) DEFAULT NULL,
    issuing_time TIMESTAMP DEFAULT NULL,
    remark text,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_change_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    reason int DEFAULT NULL,
    old_dept_id bigint DEFAULT NULL,
    new_dept_id bigint DEFAULT NULL,
    old_post_name varchar(255) DEFAULT NULL,
    new_post_name varchar(255) DEFAULT NULL,
    old_post_level varchar(255) DEFAULT NULL,
    new_post_level varchar(255) DEFAULT NULL,
    old_work_address varchar(255) DEFAULT NULL,
    new_work_address varchar(255) DEFAULT NULL,
    old_leader_employee_id bigint DEFAULT NULL,
    new_leader_employee_id bigint DEFAULT NULL,
    probation int DEFAULT NULL,
    effect_time TIMESTAMP DEFAULT NULL,
    applied_time TIMESTAMP DEFAULT NULL,
    remark text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_contact (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    relation varchar(255) DEFAULT NULL,
    phone varchar(32) DEFAULT NULL,
    work_unit varchar(255) DEFAULT NULL,
    post_name varchar(255) DEFAULT NULL,
    address varchar(512) DEFAULT NULL,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_contract (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    no varchar(64) DEFAULT NULL,
    type int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    term int DEFAULT NULL,
    status int DEFAULT NULL,
    sign_company varchar(255) DEFAULT NULL,
    sign_time TIMESTAMP DEFAULT NULL,
    remark text,
    expire_remind SMALLINT DEFAULT NULL,
    sort int DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_education_experience (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    education int DEFAULT NULL,
    graduate_school varchar(255) DEFAULT NULL,
    major varchar(255) DEFAULT NULL,
    admission_time TIMESTAMP DEFAULT NULL,
    graduation_time TIMESTAMP DEFAULT NULL,
    teaching_methods int DEFAULT NULL,
    first_degree SMALLINT DEFAULT NULL,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_file (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    url varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_personal_note (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    content text,
    reminder_time TIMESTAMP DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_quit_info (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    plan_quit_time TIMESTAMP DEFAULT NULL,
    apply_quit_time TIMESTAMP DEFAULT NULL,
    salary_settlement_time TIMESTAMP DEFAULT NULL,
    type int DEFAULT NULL,
    reason int DEFAULT NULL,
    remark text,
    old_employee_status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_salary_card (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    bank_card_number varchar(64) DEFAULT NULL,
    bank_area_id int DEFAULT NULL,
    bank_name varchar(128) DEFAULT NULL,
    bank_branch_name varchar(128) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_training_experience (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    course varchar(255) DEFAULT NULL,
    organization_name varchar(128) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    duration varchar(255) DEFAULT NULL,
    result varchar(255) DEFAULT NULL,
    certificate_name varchar(128) DEFAULT NULL,
    remark text,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_employee_wecom_sync (
    employee_id bigint NOT NULL,
    birthday TIMESTAMP DEFAULT NULL,
    entry_time TIMESTAMP DEFAULT NULL,
    synced_at TIMESTAMP NOT NULL,
    PRIMARY KEY (employee_id)
);

CREATE TABLE hrm_employee_work_experience (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    work_unit varchar(255) DEFAULT NULL,
    post_name varchar(255) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    reason text,
    witness_name varchar(255) DEFAULT NULL,
    witness_phone varchar(32) DEFAULT NULL,
    remark text,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_insurance_employee_info (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    first_social_security SMALLINT DEFAULT NULL,
    first_accumulation_fund SMALLINT DEFAULT NULL,
    social_security_number varchar(64) DEFAULT NULL,
    accumulation_fund_number varchar(64) DEFAULT NULL,
    social_security_start_month TIMESTAMP DEFAULT NULL,
    scheme_id bigint DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_insurance_month_employee_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    month_record_id bigint DEFAULT NULL,
    employee_id bigint DEFAULT NULL,
    scheme_id bigint DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    personal_insurance_amount decimal(12,2) DEFAULT NULL,
    personal_provident_fund_amount decimal(12,2) DEFAULT NULL,
    corporate_insurance_amount decimal(12,2) DEFAULT NULL,
    corporate_provident_fund_amount decimal(12,2) DEFAULT NULL,
    status int DEFAULT NULL,
    projects text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_insurance_month_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    insured_employee_count int DEFAULT NULL,
    stopped_employee_count int DEFAULT NULL,
    status int DEFAULT NULL,
    personal_insurance_amount decimal(12,2) DEFAULT NULL,
    personal_provident_fund_amount decimal(12,2) DEFAULT NULL,
    corporate_insurance_amount decimal(12,2) DEFAULT NULL,
    corporate_provident_fund_amount decimal(12,2) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_insurance_scheme (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    area_id int DEFAULT NULL,
    household_type varchar(255) DEFAULT NULL,
    type int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_insurance_scheme_project (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    scheme_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    base_amount decimal(12,2) DEFAULT NULL,
    corporate_rate decimal(10,4) DEFAULT NULL,
    personal_rate decimal(10,4) DEFAULT NULL,
    corporate_amount decimal(12,2) DEFAULT NULL,
    personal_amount decimal(12,2) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    plan_id bigint DEFAULT NULL,
    employee_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    process_status int DEFAULT NULL,
    stage_type int DEFAULT NULL,
    stage_sort int DEFAULT NULL,
    score decimal(12,2) DEFAULT NULL,
    result_level varchar(255) DEFAULT NULL,
    coefficient decimal(10,4) DEFAULT NULL,
    target_confirmation_result int DEFAULT NULL,
    target_confirmation_comment varchar(255) DEFAULT NULL,
    target_confirmation_time TIMESTAMP DEFAULT NULL,
    self_comment text,
    reviewer_comment text,
    result_comment text,
    result_confirmation_time TIMESTAMP DEFAULT NULL,
    result_audit_status int DEFAULT NULL,
    result_audit_time TIMESTAMP DEFAULT NULL,
    result_audit_reason varchar(255) DEFAULT NULL,
    appeal_reason text,
    appeal_file_urls text,
    appeal_submit_time TIMESTAMP DEFAULT NULL,
    appeal_status int DEFAULT NULL,
    appeal_time TIMESTAMP DEFAULT NULL,
    appeal_comment varchar(255) DEFAULT NULL,
    archive_time TIMESTAMP DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_action_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_id bigint DEFAULT NULL,
    stage_id bigint DEFAULT NULL,
    employee_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    content text,
    file_urls text,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_appeal_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_id bigint DEFAULT NULL,
    stage_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_dimension (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_id bigint DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    quota_type int DEFAULT NULL,
    weight decimal(10,4) DEFAULT NULL,
    remark text,
    allow_edit SMALLINT DEFAULT NULL,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_quota (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_id bigint DEFAULT NULL,
    dimension_id bigint DEFAULT NULL,
    preset SMALLINT DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    description text,
    standard varchar(255) DEFAULT NULL,
    weight decimal(10,4) DEFAULT NULL,
    score_type int DEFAULT NULL,
    target_value varchar(255) DEFAULT NULL,
    actual_value varchar(255) DEFAULT NULL,
    self_score decimal(12,2) DEFAULT NULL,
    reviewer_score decimal(12,2) DEFAULT NULL,
    final_score decimal(12,2) DEFAULT NULL,
    sort int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_quota_score (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_stage_id bigint DEFAULT NULL,
    assessment_quota_id bigint DEFAULT NULL,
    score decimal(12,2) DEFAULT NULL,
    comment varchar(255) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_stage (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    assessment_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    handler_employee_id bigint DEFAULT NULL,
    rater_type int DEFAULT NULL,
    weight decimal(10,4) DEFAULT NULL,
    scoring_type int DEFAULT NULL,
    visible_content int DEFAULT NULL,
    required_setting SMALLINT DEFAULT NULL,
    reject_authority SMALLINT DEFAULT NULL,
    sort int DEFAULT NULL,
    status int DEFAULT NULL,
    score decimal(12,2) DEFAULT NULL,
    result_level varchar(255) DEFAULT NULL,
    comment varchar(255) DEFAULT NULL,
    reject_reason text,
    submit_time TIMESTAMP DEFAULT NULL,
    deadline_time TIMESTAMP DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_assessment_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    illustrate text,
    score_calculation int DEFAULT NULL,
    upper_limit_type int DEFAULT NULL,
    upper_limit_score decimal(12,2) DEFAULT NULL,
    dimension_count int DEFAULT NULL,
    quota_count int DEFAULT NULL,
    dimensions text,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_plan (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    status int DEFAULT NULL,
    stage_type int DEFAULT NULL,
    operation_type int DEFAULT NULL,
    terminate_time TIMESTAMP DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    cycle_type int DEFAULT NULL,
    cycle varchar(255) DEFAULT NULL,
    quarter int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    description text,
    scopes text,
    assessment_template_id bigint DEFAULT NULL,
    assessment_config text,
    quota_setting_type int DEFAULT NULL,
    target_confirmation SMALLINT DEFAULT NULL,
    target_confirmation_stage text,
    review_stages text,
    result_audit SMALLINT DEFAULT NULL,
    result_audit_stages text,
    result_confirmation SMALLINT DEFAULT NULL,
    appeal_stages text,
    appeal_timeout_days int DEFAULT NULL,
    appeal_timeout_action int DEFAULT NULL,
    result_template_id bigint DEFAULT NULL,
    result_config text,
    sync_to_salary SMALLINT DEFAULT NULL,
    paid_for_month varchar(255) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_performance_result_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    status int DEFAULT NULL,
    levels text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_recruit_candidate (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    mobile varchar(32) DEFAULT NULL,
    sex int DEFAULT NULL,
    age int DEFAULT NULL,
    email varchar(128) DEFAULT NULL,
    post_id bigint DEFAULT NULL,
    stage_number int DEFAULT NULL,
    work_time int DEFAULT NULL,
    education int DEFAULT NULL,
    graduate_school varchar(255) DEFAULT NULL,
    latest_work_place varchar(255) DEFAULT NULL,
    channel_id bigint DEFAULT NULL,
    remark text,
    status int DEFAULT NULL,
    eliminate text,
    status_update_time TIMESTAMP DEFAULT NULL,
    entry_time TIMESTAMP DEFAULT NULL,
    resume_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_recruit_channel (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    system_flag SMALLINT DEFAULT NULL,
    status int DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    sort int DEFAULT NULL,
    remark text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_recruit_interview (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    candidate_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    stage_number int DEFAULT NULL,
    interview_employee_id bigint DEFAULT NULL,
    other_interview_employee_ids text,
    interview_time TIMESTAMP DEFAULT NULL,
    address varchar(512) DEFAULT NULL,
    remark text,
    result int DEFAULT NULL,
    evaluate varchar(255) DEFAULT NULL,
    cancel_reason text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_recruit_post (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    post_name varchar(128) DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    job_nature int DEFAULT NULL,
    area_id int DEFAULT NULL,
    recruit_num int DEFAULT NULL,
    reason text,
    work_time int DEFAULT NULL,
    education_require int DEFAULT NULL,
    min_salary decimal(12,2) DEFAULT NULL,
    max_salary decimal(12,2) DEFAULT NULL,
    salary_unit int DEFAULT NULL,
    min_age int DEFAULT NULL,
    max_age int DEFAULT NULL,
    latest_entry_time TIMESTAMP DEFAULT NULL,
    owner_employee_id bigint DEFAULT NULL,
    interview_employee_ids text,
    description text,
    emergency_level int DEFAULT NULL,
    post_type_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    stop_reason text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_recruit_post_type (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    parent_id bigint DEFAULT NULL,
    sort int DEFAULT NULL,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_change_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    reason int DEFAULT NULL,
    effect_time TIMESTAMP DEFAULT NULL,
    before_total decimal(12,2) DEFAULT NULL,
    after_total decimal(12,2) DEFAULT NULL,
    probation_before_total decimal(12,2) DEFAULT NULL,
    probation_after_total decimal(12,2) DEFAULT NULL,
    salary_options text,
    probation_salary_options text,
    status int DEFAULT NULL,
    remark text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_change_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    default_status SMALLINT DEFAULT NULL,
    options text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_config (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    cycle_start_day int DEFAULT NULL,
    cycle_end_day int DEFAULT NULL,
    social_security_month_type int DEFAULT NULL,
    start_year int DEFAULT NULL,
    start_month int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_employee_info (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    employee_id bigint DEFAULT NULL,
    change_reason int DEFAULT NULL,
    effect_time TIMESTAMP DEFAULT NULL,
    change_type int DEFAULT NULL,
    probation_salary decimal(12,2) DEFAULT NULL,
    regular_salary decimal(12,2) DEFAULT NULL,
    remark text,
    salary_options text,
    probation_salary_options text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_group (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    salary_standard decimal(12,2) DEFAULT NULL,
    change_rule text,
    dept_ids text,
    employee_ids text,
    tax_rule_id bigint DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_month_employee_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    month_record_id bigint DEFAULT NULL,
    employee_id bigint DEFAULT NULL,
    actual_work_day decimal(10,4) DEFAULT NULL,
    need_work_day decimal(10,4) DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    expected_pay_salary decimal(12,2) DEFAULT NULL,
    taxable_salary decimal(12,2) DEFAULT NULL,
    personal_tax decimal(12,2) DEFAULT NULL,
    real_pay_salary decimal(12,2) DEFAULT NULL,
    option_values text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_month_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    employee_count int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    expected_pay_salary decimal(12,2) DEFAULT NULL,
    personal_tax decimal(12,2) DEFAULT NULL,
    real_pay_salary decimal(12,2) DEFAULT NULL,
    personal_insurance_amount decimal(12,2) DEFAULT NULL,
    personal_provident_fund_amount decimal(12,2) DEFAULT NULL,
    corporate_insurance_amount decimal(12,2) DEFAULT NULL,
    corporate_provident_fund_amount decimal(12,2) DEFAULT NULL,
    option_headers text,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_option (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    template_id bigint DEFAULT NULL,
    code int DEFAULT NULL,
    parent_code int DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    remark text,
    system_flag SMALLINT DEFAULT NULL,
    type int DEFAULT NULL,
    visible SMALLINT DEFAULT NULL,
    enabled SMALLINT DEFAULT NULL,
    tax_enabled SMALLINT DEFAULT NULL,
    calculate_enabled SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_option_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    code int DEFAULT NULL,
    parent_code int DEFAULT NULL,
    name varchar(128) DEFAULT NULL,
    remark text,
    system_flag SMALLINT DEFAULT NULL,
    type int DEFAULT NULL,
    visible SMALLINT DEFAULT NULL,
    tax_enabled SMALLINT DEFAULT NULL,
    calculate_enabled SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_slip (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    send_record_id bigint DEFAULT NULL,
    month_employee_record_id bigint DEFAULT NULL,
    employee_id bigint DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    read_status int DEFAULT NULL,
    real_pay_salary decimal(12,2) DEFAULT NULL,
    remark text,
    options text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_slip_send_record (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    month_record_id bigint DEFAULT NULL,
    employee_count int DEFAULT NULL,
    send_employee_count int DEFAULT NULL,
    year int DEFAULT NULL,
    month int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_slip_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(128) DEFAULT NULL,
    hide_empty SMALLINT DEFAULT NULL,
    default_status SMALLINT DEFAULT NULL,
    options text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_salary_tax_rule (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name text,
    type int DEFAULT NULL,
    tax_enabled SMALLINT DEFAULT NULL,
    threshold decimal(12,2) DEFAULT NULL,
    decimal_scale int DEFAULT NULL,
    cycle_type int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE milestones (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint NOT NULL,
    type varchar(20) NOT NULL,
    milestone_date date NOT NULL,
    title varchar(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE notifications (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint NOT NULL,
    category varchar(20) NOT NULL,
    type varchar(40) NOT NULL DEFAULT '',
    title varchar(200) NOT NULL,
    link varchar(255) DEFAULT NULL,
    read_flag SMALLINT NOT NULL DEFAULT '0',
    channel varchar(20) NOT NULL DEFAULT 'site',
    dedup_key varchar(80) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE notify_preferences (
    user_id bigint NOT NULL,
    report_due SMALLINT NOT NULL DEFAULT '1',
    overdue SMALLINT NOT NULL DEFAULT '1',
    milestone SMALLINT NOT NULL DEFAULT '1',
    corp_news SMALLINT NOT NULL DEFAULT '1',
    PRIMARY KEY (user_id)
);

CREATE TABLE oa_announcement (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    type int DEFAULT NULL,
    priority int DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    content text,
    url varchar(512) DEFAULT NULL,
    top SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_announcement_receiver (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    announcement_id bigint DEFAULT NULL,
    receiver_user_id bigint DEFAULT NULL,
    read_status SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_attendance (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint DEFAULT NULL,
    type int DEFAULT NULL,
    status int DEFAULT NULL,
    attendance_time TIMESTAMP DEFAULT NULL,
    attendance_ip varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_leave_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    urgency int DEFAULT NULL,
    type int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    days int DEFAULT NULL,
    reason text,
    approvers text,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    wecom_approval_no varchar(64) DEFAULT NULL,
    wecom_request_id varchar(64) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_meeting_room (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(255) DEFAULT NULL,
    location varchar(255) DEFAULT NULL,
    type int DEFAULT NULL,
    manager_user_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    pic_url varchar(512) DEFAULT NULL,
    seat_count int DEFAULT NULL,
    equipments text,
    allow_booking SMALLINT DEFAULT NULL,
    need_approval SMALLINT DEFAULT NULL,
    booking_scope int DEFAULT NULL,
    booking_user_ids text,
    sort int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_meeting_room_booking (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    room_id bigint DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    moderator_user_id bigint DEFAULT NULL,
    attendee_user_ids text,
    reminder_type int DEFAULT NULL,
    reminded SMALLINT DEFAULT NULL,
    description text,
    dept_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    need_approval SMALLINT DEFAULT NULL,
    use_status int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_official_doc_receive (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    send_id bigint DEFAULT NULL,
    receive_type int DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    document_no varchar(64) DEFAULT NULL,
    secrecy_level int DEFAULT NULL,
    urgency_level int DEFAULT NULL,
    receive_time TIMESTAMP DEFAULT NULL,
    receive_dept_id bigint DEFAULT NULL,
    handler_user_id bigint DEFAULT NULL,
    instruction text,
    result text,
    deadline_time TIMESTAMP DEFAULT NULL,
    summary text,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    formal_file_url varchar(512) DEFAULT NULL,
    handle_status int DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_official_doc_send (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    template_id bigint DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    no_prefix varchar(64) DEFAULT NULL,
    year int DEFAULT NULL,
    sequence int DEFAULT NULL,
    document_no varchar(64) DEFAULT NULL,
    secrecy_level int DEFAULT NULL,
    urgency_level int DEFAULT NULL,
    disclosure_type int DEFAULT NULL,
    issue_time TIMESTAMP DEFAULT NULL,
    send_dept_id bigint DEFAULT NULL,
    main_dept_ids text,
    copy_dept_ids text,
    signer_user_id bigint DEFAULT NULL,
    content text,
    file_urls text,
    formal_file_url varchar(512) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_official_doc_template (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(255) DEFAULT NULL,
    authority_name varchar(255) DEFAULT NULL,
    font_size int DEFAULT NULL,
    no_prefix varchar(64) DEFAULT NULL,
    seal_pic_url varchar(512) DEFAULT NULL,
    separator_type int DEFAULT NULL,
    status int DEFAULT NULL,
    sort int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_overtime_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    urgency int DEFAULT NULL,
    type int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    days decimal(10,2) DEFAULT NULL,
    reason text,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_regular_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    urgency int DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    days int DEFAULT NULL,
    experience text,
    understanding text,
    growth text,
    deficiency text,
    improvement text,
    suggestion text,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_reimbursement (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    urgency int DEFAULT NULL,
    reason text,
    witness_user_id bigint DEFAULT NULL,
    customer_name varchar(255) DEFAULT NULL,
    payment_method int DEFAULT NULL,
    invoice_count int DEFAULT NULL,
    total_price decimal(10,2) DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    items text,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_resign_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(255) DEFAULT NULL,
    urgency int DEFAULT NULL,
    reason text,
    handover_user_id bigint DEFAULT NULL,
    unfinished_work text,
    has_pending_reimbursement SMALLINT DEFAULT NULL,
    suggestion text,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_schedule (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    type int DEFAULT NULL,
    priority int DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    description text,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    remind SMALLINT DEFAULT NULL,
    reminded SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_schedule_participant (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    schedule_id bigint DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    read_status SMALLINT DEFAULT NULL,
    read_time TIMESTAMP DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_seal (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    dept_id bigint DEFAULT NULL,
    no varchar(64) DEFAULT NULL,
    name varchar(255) DEFAULT NULL,
    type int DEFAULT NULL,
    category int DEFAULT NULL,
    keeper_user_id bigint DEFAULT NULL,
    keeper_dept_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    purchase_time TIMESTAMP DEFAULT NULL,
    enable_time TIMESTAMP DEFAULT NULL,
    disable_time TIMESTAMP DEFAULT NULL,
    pic_url varchar(512) DEFAULT NULL,
    sort int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_seal_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    seal_id bigint DEFAULT NULL,
    seal_no varchar(64) DEFAULT NULL,
    seal_name varchar(255) DEFAULT NULL,
    seal_type int DEFAULT NULL,
    keeper_dept_id bigint DEFAULT NULL,
    keeper_user_id bigint DEFAULT NULL,
    reason text,
    type int DEFAULT NULL,
    mode int DEFAULT NULL,
    document_title varchar(255) DEFAULT NULL,
    document_type varchar(255) DEFAULT NULL,
    document_count int DEFAULT NULL,
    contract_price decimal(10,2) DEFAULT NULL,
    contract_party varchar(255) DEFAULT NULL,
    expected_use_time TIMESTAMP DEFAULT NULL,
    actual_use_time TIMESTAMP DEFAULT NULL,
    expected_return_time TIMESTAMP DEFAULT NULL,
    actual_return_time TIMESTAMP DEFAULT NULL,
    use_status int DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    urgent SMALLINT DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_supply_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    apply_time TIMESTAMP DEFAULT NULL,
    use_type int DEFAULT NULL,
    pickup_method int DEFAULT NULL,
    reason text,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    file_urls text,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_supply_apply_item (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    apply_id bigint DEFAULT NULL,
    item_id bigint DEFAULT NULL,
    item_name varchar(255) DEFAULT NULL,
    model varchar(255) DEFAULT NULL,
    unit varchar(64) DEFAULT NULL,
    manage_type int DEFAULT NULL,
    apply_quantity int DEFAULT NULL,
    issued_quantity int DEFAULT NULL,
    returned_quantity int DEFAULT NULL,
    status int DEFAULT NULL,
    issue_user_id bigint DEFAULT NULL,
    issue_time TIMESTAMP DEFAULT NULL,
    issue_remark varchar(512) DEFAULT NULL,
    return_remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_supply_item (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    dept_id bigint DEFAULT NULL,
    name varchar(255) DEFAULT NULL,
    no varchar(64) DEFAULT NULL,
    category int DEFAULT NULL,
    manage_type int DEFAULT NULL,
    model varchar(255) DEFAULT NULL,
    unit varchar(64) DEFAULT NULL,
    reference_price decimal(10,2) DEFAULT NULL,
    stock_quantity int DEFAULT NULL,
    min_stock_quantity int DEFAULT NULL,
    pic_url varchar(512) DEFAULT NULL,
    status int DEFAULT NULL,
    sort int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_task (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    type int DEFAULT NULL,
    status int DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    description text,
    comment text,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    publish_time TIMESTAMP DEFAULT NULL,
    top SMALLINT DEFAULT NULL,
    canceled SMALLINT DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_task_log (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    task_id bigint DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    content text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_task_receiver (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    task_id bigint DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    status int DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_travel_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    reason text,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    days int DEFAULT NULL,
    companion varchar(255) DEFAULT NULL,
    estimated_price decimal(10,2) DEFAULT NULL,
    reimburse_status SMALLINT DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    items text,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_travel_reimbursement (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    travel_apply_id bigint DEFAULT NULL,
    reason text,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    days int DEFAULT NULL,
    total_price decimal(10,2) DEFAULT NULL,
    pay_status SMALLINT DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    items text,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_vehicle (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    name varchar(255) DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    type varchar(64) DEFAULT NULL,
    category varchar(64) DEFAULT NULL,
    brand_model varchar(255) DEFAULT NULL,
    seat_count int DEFAULT NULL,
    bare_price decimal(10,2) DEFAULT NULL,
    compulsory_insurance_expire_time TIMESTAMP DEFAULT NULL,
    commercial_insurance_expire_time TIMESTAMP DEFAULT NULL,
    inspection_expire_time TIMESTAMP DEFAULT NULL,
    pic_url varchar(512) DEFAULT NULL,
    status int DEFAULT NULL,
    sort int DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_vehicle_apply (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    vehicle_id bigint DEFAULT NULL,
    vehicle_no varchar(64) DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    start_location varchar(255) DEFAULT NULL,
    end_location varchar(255) DEFAULT NULL,
    reason text,
    passenger varchar(255) DEFAULT NULL,
    status int DEFAULT NULL,
    return_status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_vehicle_return (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    apply_id bigint DEFAULT NULL,
    vehicle_id bigint DEFAULT NULL,
    user_id bigint DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    actual_start_time TIMESTAMP DEFAULT NULL,
    start_location varchar(255) DEFAULT NULL,
    reason text,
    passenger varchar(255) DEFAULT NULL,
    actual_return_time TIMESTAMP DEFAULT NULL,
    return_location varchar(255) DEFAULT NULL,
    status int DEFAULT NULL,
    process_instance_id varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    file_urls text,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_work_report (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    no varchar(64) DEFAULT NULL,
    type int DEFAULT NULL,
    status int DEFAULT NULL,
    dept_id bigint DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    title varchar(255) DEFAULT NULL,
    summary text,
    plan text,
    problem text,
    work_items text,
    plan_items text,
    file_urls text,
    remark varchar(512) DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE oa_work_report_comment (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    report_id bigint NOT NULL,
    reviewer_id bigint NOT NULL,
    grade varchar(20) DEFAULT NULL,
    review_text varchar(1000) DEFAULT NULL,
    reply varchar(1000) DEFAULT NULL,
    reply_time TIMESTAMP DEFAULT NULL,
    creator varchar(64) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE portal_categories (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    scope varchar(32) NOT NULL,
    name varchar(100) NOT NULL,
    code varchar(32) DEFAULT NULL,
    parent_id bigint DEFAULT NULL,
    depth smallint NOT NULL DEFAULT '1',
    sort_order int NOT NULL DEFAULT '0',
    attrs TEXT NOT NULL DEFAULT ('{}'),
    archived_at TIMESTAMP DEFAULT NULL,
    archived_by varchar(64) DEFAULT NULL,
    created_by varchar(64) NOT NULL,
    active_name varchar(100) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL THEN LOWER(name) ELSE NULL END) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_categories_depth_parent CHECK (((parent_id is null) or (depth > 1))),
    CHECK (attrs IS NULL OR jsonb_typeof(attrs::jsonb) IS NOT NULL)
);

CREATE TABLE portal_news (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(200) NOT NULL,
    summary text,
    content TEXT NOT NULL,
    category varchar(50) DEFAULT NULL,
    cover_image varchar(255) DEFAULT NULL,
    template_type varchar(32) NOT NULL DEFAULT 'simple',
    blocks TEXT DEFAULT NULL,
    author varchar(100) DEFAULT NULL,
    author_username varchar(50) DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'draft',
    is_featured SMALLINT NOT NULL DEFAULT '0',
    published_at TIMESTAMP DEFAULT NULL,
    pending_notify_wecom SMALLINT NOT NULL DEFAULT '0',
    pending_notify_dept_scope TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_news_status CHECK ((status in ('draft','pending','published','rejected'))),
    CHECK (blocks IS NULL OR jsonb_typeof(blocks::jsonb) IS NOT NULL),
    CHECK (pending_notify_dept_scope IS NULL OR jsonb_typeof(pending_notify_dept_scope::jsonb) IS NOT NULL)
);

CREATE TABLE portal_news_comment (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    news_id bigint NOT NULL,
    username varchar(50) NOT NULL,
    display_name varchar(100) DEFAULT NULL,
    avatar_url varchar(255) DEFAULT NULL,
    content text NOT NULL,
    parent_id bigint DEFAULT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT '0',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_news_like (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    news_id bigint NOT NULL,
    username varchar(50) NOT NULL,
    display_name varchar(100) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_page_views (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    page_url varchar(500) NOT NULL,
    page_title varchar(300) DEFAULT NULL,
    username varchar(50) DEFAULT NULL,
    display_name varchar(100) DEFAULT NULL,
    department varchar(50) DEFAULT NULL,
    referrer varchar(500) DEFAULT NULL,
    user_agent varchar(500) DEFAULT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_wiki (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title varchar(200) NOT NULL,
    content TEXT,
    category varchar(50) DEFAULT NULL,
    department varchar(50) DEFAULT NULL,
    doc_url varchar(500) DEFAULT NULL,
    tags TEXT DEFAULT NULL,
    author_username varchar(50) DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'draft',
    view_count int NOT NULL DEFAULT '0',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_wiki_status CHECK ((status in ('draft','pending','published','rejected'))),
    CHECK (tags IS NULL OR jsonb_typeof(tags::jsonb) IS NOT NULL)
);

CREATE TABLE recognitions (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    from_id bigint NOT NULL,
    to_id bigint NOT NULL,
    type varchar(30) NOT NULL,
    scope varchar(30) DEFAULT NULL,
    note varchar(500) DEFAULT NULL,
    period varchar(20) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE rhythm_node_follow (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint NOT NULL,
    node_id bigint NOT NULL,
    remind_before int NOT NULL DEFAULT '3',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE rhythm_nodes (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name varchar(120) NOT NULL,
    kind varchar(20) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    dept_actions text,
    note varchar(500) DEFAULT NULL,
    linked_refs varchar(500) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sys_dept (
    id bigint NOT NULL,
    parent_id bigint NOT NULL DEFAULT '0',
    name varchar(64) NOT NULL,
    path varchar(255) NOT NULL DEFAULT '',
    sort_order bigint NOT NULL DEFAULT '0',
    leader_userid varchar(128) NOT NULL DEFAULT '',
    leader_userids TEXT DEFAULT NULL,
    active SMALLINT NOT NULL DEFAULT '1',
    synced_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (leader_userids IS NULL OR jsonb_typeof(leader_userids::jsonb) IS NOT NULL)
);

CREATE TABLE sys_dict_data (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    dict_type varchar(64) NOT NULL,
    label varchar(128) NOT NULL,
    value varchar(128) NOT NULL,
    sort_order int NOT NULL DEFAULT '0',
    status SMALLINT NOT NULL DEFAULT '1',
    PRIMARY KEY (id)
);

CREATE TABLE sys_dict_type (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    type varchar(64) NOT NULL,
    name varchar(64) NOT NULL,
    status SMALLINT NOT NULL DEFAULT '1',
    remark varchar(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sys_directory_sync_log (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    trigger_type varchar(16) NOT NULL,
    actor varchar(64) NOT NULL DEFAULT '',
    ok SMALLINT DEFAULT NULL,
    user_created int NOT NULL DEFAULT '0',
    user_updated int NOT NULL DEFAULT '0',
    departments int NOT NULL DEFAULT '0',
    conflicts int NOT NULL DEFAULT '0',
    missing int NOT NULL DEFAULT '0',
    message varchar(255) NOT NULL DEFAULT '',
    duration_ms bigint DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sys_employee_sync_job (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    actor_user_id bigint DEFAULT NULL,
    status varchar(16) NOT NULL DEFAULT 'queued',
    stage varchar(64) NOT NULL DEFAULT '',
    total int NOT NULL DEFAULT '0',
    processed int NOT NULL DEFAULT '0',
    worker_token varchar(64) DEFAULT NULL,
    lease_expires_at TIMESTAMP DEFAULT NULL,
    attempt_count int NOT NULL DEFAULT '0',
    scan_summary TEXT DEFAULT NULL,
    preview TEXT DEFAULT NULL,
    unmatched_candidates TEXT DEFAULT NULL,
    apply_summary TEXT DEFAULT NULL,
    error_code varchar(64) NOT NULL DEFAULT '',
    error_message text,
    heartbeat_at TIMESTAMP DEFAULT NULL,
    expires_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (scan_summary IS NULL OR jsonb_typeof(scan_summary::jsonb) IS NOT NULL),
    CHECK (preview IS NULL OR jsonb_typeof(preview::jsonb) IS NOT NULL),
    CHECK (unmatched_candidates IS NULL OR jsonb_typeof(unmatched_candidates::jsonb) IS NOT NULL),
    CHECK (apply_summary IS NULL OR jsonb_typeof(apply_summary::jsonb) IS NOT NULL)
);

CREATE TABLE sys_menu (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    parent_id bigint NOT NULL DEFAULT '0',
    name varchar(64) NOT NULL,
    type varchar(16) NOT NULL DEFAULT 'button',
    path varchar(191) NOT NULL DEFAULT '',
    permission varchar(128) NOT NULL DEFAULT '',
    sort_order int NOT NULL DEFAULT '0',
    visible SMALLINT NOT NULL DEFAULT '1',
    PRIMARY KEY (id)
);

CREATE TABLE sys_operate_log (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    trace_id varchar(64) NOT NULL DEFAULT '',
    user_id bigint DEFAULT NULL,
    user_type int NOT NULL DEFAULT '0',
    type varchar(64) NOT NULL DEFAULT '',
    sub_type varchar(64) NOT NULL DEFAULT '',
    biz_id bigint DEFAULT NULL,
    action varchar(512) NOT NULL DEFAULT '',
    extra text,
    target_table varchar(64) NOT NULL DEFAULT '',
    before_value text,
    after_value text,
    comment varchar(512) NOT NULL DEFAULT '',
    request_method varchar(16) NOT NULL DEFAULT '',
    request_url varchar(255) NOT NULL DEFAULT '',
    user_ip varchar(64) NOT NULL DEFAULT '',
    user_agent varchar(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sys_role (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(64) NOT NULL,
    built_in SMALLINT NOT NULL DEFAULT '0',
    remark varchar(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sys_role_menu (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    role_id bigint NOT NULL,
    menu_id bigint NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sys_user (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    wecom_userid varchar(128) DEFAULT NULL,
    username varchar(64) NOT NULL,
    name varchar(64) NOT NULL,
    position varchar(64) NOT NULL DEFAULT '',
    main_dept_id bigint DEFAULT NULL,
    mobile varchar(32) NOT NULL DEFAULT '',
    email varchar(128) NOT NULL DEFAULT '',
    avatar varchar(512) NOT NULL DEFAULT '',
    wecom_status int DEFAULT NULL,
    enabled SMALLINT NOT NULL DEFAULT '1',
    password_hash varchar(256) DEFAULT NULL,
    last_login_at TIMESTAMP DEFAULT NULL,
    directory_synced_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE sys_user_dept (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint NOT NULL,
    dept_id bigint NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sys_user_role (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE voices (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id bigint DEFAULT NULL,
    channel varchar(30) NOT NULL,
    anonymous SMALLINT NOT NULL DEFAULT '0',
    content varchar(2000) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'open',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_ai_models (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    provider VARCHAR(16) NOT NULL,
    model_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    enabled SMALLINT NOT NULL DEFAULT 1,
    input_price DECIMAL(10,4) NULL,
    output_price DECIMAL(10,4) NULL,
    note VARCHAR(200) NULL,
    sort_order BIGINT NOT NULL DEFAULT '0',
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_ai_provider_settings (
    provider VARCHAR(16) NOT NULL,
    is_default SMALLINT NOT NULL DEFAULT 0,
    tier_fast VARCHAR(64) NULL,
    tier_standard VARCHAR(64) NULL,
    tier_quality VARCHAR(64) NULL,
    updated_by VARCHAR(50) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    api_key_ciphertext TEXT NULL,
    api_key_hint VARCHAR(8) NULL,
    api_key_updated_by VARCHAR(50) NULL,
    api_key_updated_at TIMESTAMP NULL,
    PRIMARY KEY (provider)
);

CREATE TABLE portal_ai_task_settings (
    task_name VARCHAR(64) NOT NULL,
    provider VARCHAR(16) NULL,
    model VARCHAR(64) NULL,
    effort VARCHAR(8) NULL,
    system_prompt TEXT NULL,
    user_template TEXT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_name)
);

CREATE TABLE portal_ai_task_setting_versions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    task_name VARCHAR(64) NOT NULL,
    version_no BIGINT NOT NULL,
    provider VARCHAR(16) NULL,
    model VARCHAR(64) NULL,
    effort VARCHAR(8) NULL,
    system_prompt TEXT NULL,
    user_template TEXT NULL,
    note VARCHAR(200) NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_api_call_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NULL,
    endpoint VARCHAR(128) NOT NULL,
    tokens_in BIGINT NULL,
    tokens_out BIGINT NULL,
    cost_usd DECIMAL(10,6) NULL,
    latency_ms BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ok',
    error_msg TEXT NULL,
    called_by VARCHAR(50) NULL,
    called_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_entry_rollout (
    id SMALLINT NOT NULL PRIMARY KEY,
    entries TEXT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_by VARCHAR(100) NULL,
    updated_at TIMESTAMP(6) NULL,
    CHECK (entries IS NULL OR jsonb_typeof(entries::jsonb) IS NOT NULL)
);

CREATE TABLE portal_esports_settings (
    id                   BIGINT      NOT NULL,
    is_open              SMALLINT  NOT NULL DEFAULT 0,
    open_weekdays        TEXT        NOT NULL,
    open_start_time      TIME        NOT NULL DEFAULT '18:00:00',
    open_end_time        TIME        NOT NULL DEFAULT '22:00:00',
    advance_days         INT         NOT NULL DEFAULT 7,
    max_duration_minutes INT         NOT NULL DEFAULT 120,
    approver_user_id     BIGINT      NULL,
    approval_mode        VARCHAR(16) NOT NULL DEFAULT 'any',
    approval_stages      TEXT        NOT NULL,
    updated_by           VARCHAR(50) NULL,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_esports_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_esports_settings_time CHECK (open_start_time < open_end_time),
    CONSTRAINT ck_esports_settings_advance CHECK (advance_days BETWEEN 0 AND 90),
    CONSTRAINT ck_esports_settings_duration CHECK (max_duration_minutes BETWEEN 1 AND 1440),
    CONSTRAINT ck_esports_settings_approval_mode CHECK (approval_mode IN ('any', 'sequential')),
    CHECK (open_weekdays IS NULL OR jsonb_typeof(open_weekdays::jsonb) IS NOT NULL),
    CHECK (approval_stages IS NULL OR jsonb_typeof(approval_stages::jsonb) IS NOT NULL)
);

CREATE TABLE portal_esports_bookings (
    id                   BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    booking_date         DATE         NOT NULL,
    start_time           TIME         NOT NULL,
    end_time             TIME         NOT NULL,
    title                VARCHAR(120) NOT NULL,
    purpose              VARCHAR(1000) NULL,
    participant_count    INT          NOT NULL DEFAULT 1,
    participants         TEXT         NULL,
    applicant_user_id    BIGINT       NOT NULL,
    applicant_name       VARCHAR(100) NOT NULL,
    approver_user_id     BIGINT       NOT NULL,
    approver_name        VARCHAR(100) NOT NULL,
    approval_mode        VARCHAR(16)  NOT NULL DEFAULT 'any',
    current_stage_index  INT          NULL DEFAULT 0,
    status               VARCHAR(16)  NOT NULL DEFAULT 'pending',
    reviewed_by_user_id  BIGINT       NULL,
    reviewed_by_name     VARCHAR(100) NULL,
    review_remark        VARCHAR(1000) NULL,
    reviewed_at          TIMESTAMP     NULL,
    cancelled_by_user_id BIGINT       NULL,
    cancelled_at         TIMESTAMP     NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_esports_bookings_time CHECK (start_time < end_time),
    CONSTRAINT ck_esports_bookings_participants CHECK (participant_count BETWEEN 1 AND 100),
    CONSTRAINT ck_esports_bookings_status CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
    CONSTRAINT ck_esports_bookings_approval_mode CHECK (approval_mode IN ('any', 'sequential')),
    CONSTRAINT ck_esports_bookings_current_stage CHECK (current_stage_index IS NULL OR current_stage_index BETWEEN 0 AND 9),
    CHECK (participants IS NULL OR jsonb_typeof(participants::jsonb) IS NOT NULL)
);

CREATE TABLE portal_esports_approval_steps (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    booking_id       BIGINT       NOT NULL,
    stage_index      INT          NOT NULL,
    approver_user_id BIGINT       NOT NULL,
    approver_name    VARCHAR(100) NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'waiting',
    acted_by_user_id BIGINT       NULL,
    acted_by_name    VARCHAR(100) NULL,
    acted_at         TIMESTAMP     NULL,
    remark           VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_esports_approval_steps_stage CHECK (stage_index BETWEEN 0 AND 9),
    CONSTRAINT ck_esports_approval_steps_status CHECK (status IN ('waiting', 'pending', 'approved', 'rejected', 'skipped'))
);

CREATE TABLE portal_esports_booking_logs (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    booking_id    BIGINT       NOT NULL,
    operation     VARCHAR(16)  NOT NULL,
    actor_user_id BIGINT       NOT NULL,
    actor_name    VARCHAR(100) NOT NULL,
    remark        VARCHAR(1000) NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_esports_booking_logs_operation CHECK (operation IN ('submit', 'approve', 'reject', 'cancel'))
);

CREATE TABLE portal_events (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NULL,
    location    VARCHAR(200) NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NULL,
    event_type  VARCHAR(50)  NOT NULL DEFAULT 'exhibition',
    status      VARCHAR(20)  NOT NULL DEFAULT 'published',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_products (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    brand       VARCHAR(50)  NULL,
    category    VARCHAR(50)  NULL,
    description TEXT         NULL,
    image_url   VARCHAR(500) NULL,
    link_url    VARCHAR(500) NULL,
    launch_date DATE         NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'published',
    is_featured SMALLINT   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_feedback (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    reporter_name VARCHAR(100) NOT NULL,
    department    VARCHAR(100) NULL,
    category      VARCHAR(50)  NOT NULL,
    sub_category  VARCHAR(100) NULL,
    brand         VARCHAR(50)  NULL,
    product_name  VARCHAR(200) NULL,
    sentiment     VARCHAR(20)  NULL,
    priority      VARCHAR(20)  NOT NULL DEFAULT 'normal',
    raw_message   TEXT   NOT NULL,
    image_urls    TEXT         NULL,
    ai_summary    TEXT         NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'pending',
    assigned_to   VARCHAR(100) NULL,
    resolved_note TEXT         NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (image_urls IS NULL OR jsonb_typeof(image_urls::jsonb) IS NOT NULL)
);

CREATE TABLE portal_hr_layout_jobs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    source_type VARCHAR(16) NOT NULL DEFAULT 'paste',
    source_name VARCHAR(255) NULL,
    input_text TEXT NOT NULL,
    input_chars INT NOT NULL,
    progress SMALLINT NOT NULL DEFAULT 0,
    stage VARCHAR(128) NOT NULL DEFAULT '等待处理',
    summary TEXT NULL,
    blocks TEXT NULL,
    error TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    PRIMARY KEY(id),
    CHECK (blocks IS NULL OR jsonb_typeof(blocks::jsonb) IS NOT NULL)
);

CREATE TABLE portal_hr_layout_attempts (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
);

CREATE TABLE portal_hr_documents (
    id                          BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    slug                        VARCHAR(64)  NULL,
    title                       VARCHAR(200) NOT NULL,
    category                    VARCHAR(32)  NOT NULL,
    topic                       VARCHAR(32)  NULL,
    content_html                TEXT   NULL,
    draft_content               TEXT   NULL,
    content_blocks              TEXT         NULL,
    draft_blocks                TEXT         NULL,
    version                     VARCHAR(16)  NOT NULL DEFAULT 'v1.0',
    draft_version               VARCHAR(16)  NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'draft',
    requires_signoff            SMALLINT   NOT NULL DEFAULT 0,
    dept_scope                  TEXT         NULL,
    draft_title                 VARCHAR(200) NULL,
    draft_requires_signoff      SMALLINT   NULL,
    draft_dept_scope            TEXT         NULL,
    intro                       TEXT         NULL,
    draft_intro                 TEXT         NULL,
    cover_image                 VARCHAR(500) NULL,
    draft_cover_image           VARCHAR(500) NULL,
    theme                       VARCHAR(32)  NULL,
    draft_theme                 VARCHAR(32)  NULL,
    tags                        TEXT         NULL,
    draft_tags                  TEXT         NULL,
    toc_enabled                 SMALLINT   NOT NULL DEFAULT 1,
    draft_toc_enabled           SMALLINT   NULL,
    toc_position                VARCHAR(8)   NOT NULL DEFAULT 'left',
    draft_toc_position          VARCHAR(8)   NULL,
    cover                       TEXT         NULL,
    draft_cover                 TEXT         NULL,
    download_pdf_enabled        SMALLINT   NOT NULL DEFAULT 0,
    draft_download_pdf_enabled  SMALLINT   NULL,
    signoff                     TEXT         NULL,
    draft_signoff               TEXT         NULL,
    updated_by                  VARCHAR(50)  NULL,
    updated_at                  TIMESTAMP     NULL,
    submitted_by                VARCHAR(50)  NULL,
    submitted_at                TIMESTAMP     NULL,
    published_by                VARCHAR(50)  NULL,
    published_at                TIMESTAMP     NULL,
    offline_at                  TIMESTAMP     NULL,
    offline_by                  VARCHAR(50)  NULL,
    offline_reason              TEXT         NULL,
    deleted_at                  TIMESTAMP     NULL,
    deleted_by                  VARCHAR(50)  NULL,
    created_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_hr_documents_status CHECK (status IN ('draft', 'pending', 'published', 'rejected')),
    CHECK (content_blocks IS NULL OR jsonb_typeof(content_blocks::jsonb) IS NOT NULL),
    CHECK (draft_blocks IS NULL OR jsonb_typeof(draft_blocks::jsonb) IS NOT NULL),
    CHECK (dept_scope IS NULL OR jsonb_typeof(dept_scope::jsonb) IS NOT NULL),
    CHECK (draft_dept_scope IS NULL OR jsonb_typeof(draft_dept_scope::jsonb) IS NOT NULL),
    CHECK (tags IS NULL OR jsonb_typeof(tags::jsonb) IS NOT NULL),
    CHECK (draft_tags IS NULL OR jsonb_typeof(draft_tags::jsonb) IS NOT NULL),
    CHECK (cover IS NULL OR jsonb_typeof(cover::jsonb) IS NOT NULL),
    CHECK (draft_cover IS NULL OR jsonb_typeof(draft_cover::jsonb) IS NOT NULL),
    CHECK (signoff IS NULL OR jsonb_typeof(signoff::jsonb) IS NOT NULL),
    CHECK (draft_signoff IS NULL OR jsonb_typeof(draft_signoff::jsonb) IS NOT NULL)
);

CREATE TABLE portal_hr_document_aliases (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    alias        VARCHAR(255) NOT NULL,
    alias_type   VARCHAR(20)  NOT NULL,
    document_id  BIGINT       NOT NULL,
    created_by   VARCHAR(50)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_hr_document_aliases_type CHECK (alias_type IN ('slug', 'legacy_key', 'legacy_path'))
);

CREATE TABLE portal_hr_document_entry_bindings (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY      NOT NULL,
    entry_key    VARCHAR(64) NOT NULL,
    document_id  BIGINT      NOT NULL,
    created_by   VARCHAR(50) NULL,
    updated_by   VARCHAR(50) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_hr_document_versions (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    document_id    BIGINT       NOT NULL,
    slug           VARCHAR(64)  NULL,
    version        VARCHAR(16)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content_html   TEXT   NOT NULL,
    content_blocks TEXT         NULL,
    published_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by   VARCHAR(50)  NULL,
    approved_by    VARCHAR(50)  NULL,
    submitted_by   VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    CHECK (content_blocks IS NULL OR jsonb_typeof(content_blocks::jsonb) IS NOT NULL)
);

CREATE TABLE portal_hr_document_draft_versions (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    document_id    BIGINT       NOT NULL,
    slug           VARCHAR(64)  NULL,
    title          VARCHAR(200) NOT NULL,
    content_html   TEXT   NULL,
    content_blocks TEXT         NULL,
    saved_by       VARCHAR(50)  NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (content_blocks IS NULL OR jsonb_typeof(content_blocks::jsonb) IS NOT NULL)
);

CREATE TABLE portal_content_push_jobs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    push_trigger VARCHAR(32) NOT NULL,
    target_key VARCHAR(191) NOT NULL,
    content TEXT NOT NULL,
    recipients TEXT NOT NULL,
    pushed_by VARCHAR(64) NOT NULL,
    force_send SMALLINT NOT NULL DEFAULT 0,
    target_count INT NOT NULL,
    sent_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'queued',
    error_msg VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    CHECK (recipients IS NULL OR jsonb_typeof(recipients::jsonb) IS NOT NULL)
);

CREATE TABLE portal_content_push_guard (
    push_trigger VARCHAR(32) NOT NULL,
    target_key VARCHAR(191) NOT NULL,
    last_success_at TIMESTAMP NULL,
    PRIMARY KEY(push_trigger,target_key)
);

CREATE TABLE portal_signoff_notify_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    actor_id BIGINT NULL,
    document_id BIGINT NULL,
    target_count INT NOT NULL,
    sent_count INT NOT NULL,
    failed_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_brand_docs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    brand_code VARCHAR(32) NOT NULL,
    doc_type VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    file_url TEXT NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'published',
    updated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_brand_doc_versions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    file_url TEXT NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    uploaded_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_contract_entities (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    legal_rep VARCHAR(50) NOT NULL,
    is_active SMALLINT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_celebration_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    enabled SMALLINT NOT NULL DEFAULT 0,
    send_hour INT NOT NULL DEFAULT 9,
    send_minute INT NOT NULL DEFAULT 0,
    birthday_enabled SMALLINT NOT NULL DEFAULT 1,
    birthday_card_enabled SMALLINT NOT NULL DEFAULT 0,
    templates TEXT NULL,
    updated_by VARCHAR(64) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (templates IS NULL OR jsonb_typeof(templates::jsonb) IS NOT NULL)
);

CREATE TABLE portal_celebration_preferences (
    user_id BIGINT NOT NULL PRIMARY KEY,
    opt_out SMALLINT NOT NULL DEFAULT 0,
    birthday_override SMALLINT NOT NULL DEFAULT 0,
    birthday_month_day VARCHAR(5) NULL
);

CREATE TABLE portal_resume_interview_processes (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    position VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    result VARCHAR(16) NULL,
    summary TEXT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    active_resume_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='active' THEN resume_id ELSE NULL END) STORED
);

CREATE TABLE portal_resume_interview_rounds (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    process_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    interviewer VARCHAR(100) NULL,
    scheduled_at TIMESTAMP NULL,
    location VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    result VARCHAR(16) NULL,
    feedback TEXT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_celebration_runs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    run_date DATE NOT NULL,
    kind VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL,
    detail TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_celebration_push_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    user_id BIGINT NULL,
    kind VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    sent_ok SMALLINT NOT NULL DEFAULT 0,
    error_msg VARCHAR(500) NULL,
    source_log_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_wecom_push_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    trigger VARCHAR(32) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    pushed_by VARCHAR(64) NOT NULL,
    pushed_at TIMESTAMP NOT NULL,
    error_msg TEXT NULL
);

CREATE TABLE portal_celebration_cards (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    token VARCHAR(32) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    recipient_username VARCHAR(64) NULL,
    recipient_name VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    years INT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'html',
    image_key VARCHAR(255) NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_ok SMALLINT NOT NULL DEFAULT 0,
    error_msg VARCHAR(500) NULL,
    sent_at TIMESTAMP NULL
);

CREATE TABLE portal_resumes (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    source_filename VARCHAR(255) NOT NULL,
    file_ext VARCHAR(10) NOT NULL,
    file_size BIGINT NOT NULL,
    file_sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(255) NULL,
    name VARCHAR(100) NULL,
    gender VARCHAR(4) NULL,
    birth_date DATE NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(120) NULL,
    current_city VARCHAR(50) NULL,
    highest_degree VARCHAR(20) NULL,
    years_of_experience DECIMAL(4,1) NULL,
    current_company VARCHAR(120) NULL,
    current_title VARCHAR(120) NULL,
    expected_position VARCHAR(120) NULL,
    expected_salary VARCHAR(60) NULL,
    summary TEXT NULL,
    extracted TEXT NOT NULL,
    ai_raw TEXT NULL,
    ai_model VARCHAR(64) NULL,
    ai_tokens_in BIGINT NULL,
    ai_tokens_out BIGINT NULL,
    ai_request_id VARCHAR(64) NULL,
    error_msg TEXT NULL,
    duplicate_of_id BIGINT NULL,
    uploaded_by VARCHAR(64) NOT NULL,
    confirmed_by VARCHAR(64) NULL,
    confirmed_at TIMESTAMP NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (extracted IS NULL OR jsonb_typeof(extracted::jsonb) IS NOT NULL),
    CHECK (ai_raw IS NULL OR jsonb_typeof(ai_raw::jsonb) IS NOT NULL)
);

CREATE TABLE portal_material_site_owners (
    site VARCHAR(8) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    owner_username VARCHAR(64) NOT NULL,
    owner_name VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NULL,
    updated_by_name VARCHAR(64) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (site,kind)
);

CREATE TABLE portal_material_products (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    kind VARCHAR(16) NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(50) NULL,
    description TEXT NULL,
    image_urls TEXT NOT NULL DEFAULT ('[]'),
    variant_dims TEXT NOT NULL DEFAULT ('[]'),
    archived_at TIMESTAMP NULL,
    archived_by VARCHAR(64) NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    archive_group CHAR(36) NULL,
    active_name VARCHAR(200) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL THEN LOWER(TRIM(name)) ELSE NULL END) STORED,
    active_brand VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL THEN LOWER(TRIM(COALESCE(brand,''))) ELSE NULL END) STORED,
    CHECK (image_urls IS NULL OR jsonb_typeof(image_urls::jsonb) IS NOT NULL),
    CHECK (variant_dims IS NULL OR jsonb_typeof(variant_dims::jsonb) IS NOT NULL)
);

CREATE TABLE portal_materials (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    kind VARCHAR(16) NOT NULL DEFAULT 'sample',
    product_id BIGINT NULL,
    variant_values TEXT NOT NULL DEFAULT ('{}'),
    site VARCHAR(8) NOT NULL DEFAULT 'SZ',
    category VARCHAR(32) NOT NULL,
    sku VARCHAR(64) NULL,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(50) NULL,
    spec VARCHAR(64) NULL,
    unit_price DECIMAL(10,2) NULL,
    image_urls TEXT NOT NULL DEFAULT ('[]'),
    stock_qty BIGINT NOT NULL DEFAULT '1',
    location VARCHAR(64) NOT NULL DEFAULT '陈列室',
    current_borrowers TEXT NOT NULL DEFAULT ('[]'),
    owner_username VARCHAR(64) NOT NULL,
    owner_name VARCHAR(64) NOT NULL,
    note TEXT NULL,
    archived_at TIMESTAMP NULL,
    archived_by VARCHAR(64) NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK(stock_qty >= 0),
    archive_group CHAR(36) NULL,
    active_variant CHAR(64) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL AND variant_values::jsonb <> '{}'::jsonb AND variant_values::jsonb <> '[]'::jsonb THEN cyt_json_fingerprint(variant_values) ELSE NULL END) STORED,
    CHECK (variant_values IS NULL OR jsonb_typeof(variant_values::jsonb) IS NOT NULL),
    CHECK (image_urls IS NULL OR jsonb_typeof(image_urls::jsonb) IS NOT NULL),
    CHECK (current_borrowers IS NULL OR jsonb_typeof(current_borrowers::jsonb) IS NOT NULL)
);

CREATE TABLE portal_material_applies (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    apply_no VARCHAR(32) NOT NULL,
    apply_type VARCHAR(16) NOT NULL DEFAULT 'sample_borrow',
    applicant_username VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(64) NOT NULL,
    department VARCHAR(64) NULL,
    purpose VARCHAR(200) NOT NULL DEFAULT '',
    expect_return_date DATE NULL,
    actual_return_date DATE NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    remark TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_material_apply_items (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    apply_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(200) NOT NULL,
    material_sku VARCHAR(64) NOT NULL DEFAULT '',
    spec VARCHAR(64) NULL,
    quantity BIGINT NOT NULL,
    approved_qty BIGINT NULL,
    returned_quantity BIGINT NOT NULL DEFAULT '0',
    PRIMARY KEY (id),
    CHECK(quantity > 0),
    CHECK(approved_qty IS NULL OR (approved_qty>=0 AND approved_qty<=quantity)),
    CHECK(returned_quantity>=0 AND returned_quantity<=COALESCE(approved_qty,quantity))
);

CREATE TABLE portal_material_apply_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    apply_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL,
    operator_username VARCHAR(64) NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    remark TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE portal_handbook_signoff (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    user_id     BIGINT       NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    document_id BIGINT       NULL,
    document    VARCHAR(64)  NOT NULL,
    version     VARCHAR(16)  NOT NULL,
    signed_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip          VARCHAR(64)  NULL,
    user_agent  VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE portal_training (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NULL,
    department      VARCHAR(50)  NULL,
    level           VARCHAR(20)  NOT NULL DEFAULT 'elective',
    duration_minutes INT         NULL,
    instructor      VARCHAR(100) NULL,
    doc_url         VARCHAR(500) NULL,
    video_url       VARCHAR(500) NULL,
    cover_image     VARCHAR(255) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    sort_order      INT          NOT NULL DEFAULT 0,
    category        VARCHAR(100) NULL,
    category_id     BIGINT       NULL,
    source_prefix   VARCHAR(500) NULL,
    dept_scope      TEXT         NULL,
    author_username VARCHAR(50)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_training_level CHECK (level IN ('required', 'elective')),
    CONSTRAINT ck_training_status CHECK (status IN ('draft', 'published')),
    CHECK (dept_scope IS NULL OR jsonb_typeof(dept_scope::jsonb) IS NOT NULL)
);

CREATE TABLE portal_training_lessons (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    course_id     BIGINT       NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    title         VARCHAR(200) NOT NULL,
    kind          VARCHAR(20)  NOT NULL,
    object_key    VARCHAR(500) NULL,
    pdf_key       VARCHAR(500) NULL,
    pages         TEXT         NULL,
    url           VARCHAR(500) NULL,
    duration_sec  INT          NULL,
    size_bytes    BIGINT       NULL,
    source_prefix VARCHAR(500) NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_training_lessons_kind CHECK (kind IN ('video', 'pdf', 'article', 'cards', 'link')),
    CHECK (pages IS NULL OR jsonb_typeof(pages::jsonb) IS NOT NULL)
);

CREATE TABLE portal_training_lesson_progress (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    lesson_id    BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'started',
    position     INT          NOT NULL DEFAULT 0,
    percent      INT          NOT NULL DEFAULT 0,
    duration_sec INT          NULL,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP     NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_training_lesson_progress_status CHECK (status IN ('started', 'completed')),
    CONSTRAINT ck_training_lesson_progress_percent CHECK (percent >= 0 AND percent <= 100)
);

CREATE TABLE portal_training_progress (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY       NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    training_id  BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'started',
    started_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP     NULL,
    PRIMARY KEY (id)
);

CREATE TABLE hrm_contract_template (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(40) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    contract_type INT NOT NULL DEFAULT 1,
    scope VARCHAR(255) NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewer BIGINT NULL,
    review_note VARCHAR(500) NULL,
    creator BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL
);

CREATE TABLE hrm_employment_document (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    request_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    employee_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    effective_date DATE NOT NULL,
    account_disabled SMALLINT NOT NULL DEFAULT 0,
    creator BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hrm_candidate_attachment (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE portal_workspace_records (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    kind VARCHAR(32) NOT NULL,
    record_key VARCHAR(100) NULL,
    owner_id BIGINT NOT NULL,
    dept_id BIGINT NULL,
    parent_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    payload TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (payload IS NULL OR jsonb_typeof(payload::jsonb) IS NOT NULL)
);

CREATE TABLE portal_workspace_answers (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    survey_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    answers TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CHECK (answers IS NULL OR jsonb_typeof(answers::jsonb) IS NOT NULL)
);

CREATE TABLE portal_workspace_approval_steps (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    request_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    approver_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'waiting',
    comment VARCHAR(1000) NULL,
    acted_at TIMESTAMP NULL,
    PRIMARY KEY(id)
);

CREATE TABLE hrm_wecom_roster_sync_run (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    stage VARCHAR(16) NOT NULL,
    actor VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL DEFAULT '',
    directory_json TEXT NULL,
    result_json TEXT NULL,
    active_slot SMALLINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6) NULL
);

CREATE TABLE oa_wecom_approval_link (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    apply_type VARCHAR(32) NOT NULL,
    biz_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL DEFAULT '',
    approval_no VARCHAR(64) NOT NULL DEFAULT '',
    creator VARCHAR(32) DEFAULT '',
    updater VARCHAR(32) DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX flow_definition_idx_flow_code ON flow_definition (flow_code);

CREATE INDEX flow_form_idx_form_code ON flow_form (form_code);

CREATE INDEX flow_his_task_idx_instance_id ON flow_his_task (instance_id);

CREATE INDEX flow_his_task_idx_task_id ON flow_his_task (task_id);

CREATE INDEX flow_instance_idx_business_id ON flow_instance (business_id);

CREATE INDEX flow_instance_idx_definition_id ON flow_instance (definition_id);

CREATE INDEX flow_node_idx_definition_id ON flow_node (definition_id);

CREATE INDEX flow_skip_idx_definition_id ON flow_skip (definition_id);

CREATE INDEX flow_task_idx_instance_id ON flow_task (instance_id);

CREATE INDEX flow_task_idx_definition_id ON flow_task (definition_id);

CREATE INDEX flow_user_user_processed_type ON flow_user (processed_by,type);

CREATE INDEX flow_user_user_associated ON flow_user (associated);

CREATE INDEX hrm_attendance_clock_idx_clock_time ON hrm_attendance_clock (clock_time);

CREATE INDEX hrm_attendance_clock_idx_employee_id ON hrm_attendance_clock (employee_id);

CREATE INDEX hrm_attendance_clock_idx_status ON hrm_attendance_clock (status);

CREATE INDEX hrm_attendance_clock_idx_type ON hrm_attendance_clock (type);

CREATE INDEX hrm_attendance_clock_idx_create_time ON hrm_attendance_clock (create_time);

CREATE INDEX hrm_attendance_group_idx_default_status ON hrm_attendance_group (default_status);

CREATE INDEX hrm_attendance_group_idx_name ON hrm_attendance_group (name);

CREATE INDEX hrm_attendance_group_idx_create_time ON hrm_attendance_group (create_time);

CREATE INDEX hrm_attendance_holiday_idx_date ON hrm_attendance_holiday (date);

CREATE INDEX hrm_attendance_holiday_idx_type ON hrm_attendance_holiday (type);

CREATE INDEX hrm_attendance_holiday_idx_create_time ON hrm_attendance_holiday (create_time);

CREATE INDEX hrm_attendance_leave_idx_start_time ON hrm_attendance_leave (start_time);

CREATE INDEX hrm_attendance_leave_idx_employee_id ON hrm_attendance_leave (employee_id);

CREATE INDEX hrm_attendance_leave_idx_approval_status ON hrm_attendance_leave (approval_status);

CREATE INDEX hrm_attendance_leave_idx_type ON hrm_attendance_leave (type);

CREATE INDEX hrm_attendance_leave_idx_create_time ON hrm_attendance_leave (create_time);

CREATE INDEX hrm_config_idx_type ON hrm_config (type);

CREATE INDEX hrm_config_idx_sort ON hrm_config (sort);

CREATE INDEX hrm_config_idx_create_time ON hrm_config (create_time);

CREATE UNIQUE INDEX hrm_employee_uk_hrm_employee_active_user ON hrm_employee (active_user_id);

CREATE INDEX hrm_employee_idx_entry_status ON hrm_employee (entry_status);

CREATE INDEX hrm_employee_idx_job_number ON hrm_employee (job_number);

CREATE INDEX hrm_employee_idx_name ON hrm_employee (name);

CREATE INDEX hrm_employee_idx_entry_time ON hrm_employee (entry_time);

CREATE INDEX hrm_employee_idx_create_time ON hrm_employee (create_time);

CREATE INDEX hrm_employee_certificate_idx_employee_id ON hrm_employee_certificate (employee_id);

CREATE INDEX hrm_employee_certificate_idx_sort ON hrm_employee_certificate (sort);

CREATE INDEX hrm_employee_certificate_idx_create_time ON hrm_employee_certificate (create_time);

CREATE INDEX hrm_employee_change_record_idx_effect_time ON hrm_employee_change_record (effect_time);

CREATE INDEX hrm_employee_change_record_idx_type ON hrm_employee_change_record (type);

CREATE INDEX hrm_employee_change_record_idx_employee_id ON hrm_employee_change_record (employee_id);

CREATE INDEX hrm_employee_change_record_idx_create_time ON hrm_employee_change_record (create_time);

CREATE INDEX hrm_employee_contact_idx_employee_id ON hrm_employee_contact (employee_id);

CREATE INDEX hrm_employee_contact_idx_sort ON hrm_employee_contact (sort);

CREATE INDEX hrm_employee_contact_idx_create_time ON hrm_employee_contact (create_time);

CREATE INDEX hrm_employee_contract_idx_end_time ON hrm_employee_contract (end_time);

CREATE INDEX hrm_employee_contract_idx_employee_id ON hrm_employee_contract (employee_id);

CREATE INDEX hrm_employee_contract_idx_sort ON hrm_employee_contract (sort);

CREATE INDEX hrm_employee_contract_idx_expire_remind ON hrm_employee_contract (expire_remind);

CREATE INDEX hrm_employee_contract_idx_create_time ON hrm_employee_contract (create_time);

CREATE INDEX hrm_employee_education_experience_idx_employee_id ON hrm_employee_education_experience (employee_id);

CREATE INDEX hrm_employee_education_experience_idx_sort ON hrm_employee_education_experience (sort);

CREATE INDEX hrm_employee_education_experience_idx_create_time ON hrm_employee_education_experience (create_time);

CREATE INDEX hrm_employee_file_idx_employee_id ON hrm_employee_file (employee_id);

CREATE INDEX hrm_employee_file_idx_type ON hrm_employee_file (type);

CREATE INDEX hrm_employee_file_idx_create_time ON hrm_employee_file (create_time);

CREATE INDEX hrm_employee_personal_note_idx_reminder_time ON hrm_employee_personal_note (reminder_time);

CREATE INDEX hrm_employee_personal_note_idx_employee_id ON hrm_employee_personal_note (employee_id);

CREATE INDEX hrm_employee_personal_note_idx_create_time ON hrm_employee_personal_note (create_time);

CREATE INDEX hrm_employee_quit_info_idx_plan_quit_time ON hrm_employee_quit_info (plan_quit_time);

CREATE INDEX hrm_employee_quit_info_idx_employee_id ON hrm_employee_quit_info (employee_id);

CREATE INDEX hrm_employee_quit_info_idx_create_time ON hrm_employee_quit_info (create_time);

CREATE INDEX hrm_employee_salary_card_idx_employee_id ON hrm_employee_salary_card (employee_id);

CREATE INDEX hrm_employee_salary_card_idx_create_time ON hrm_employee_salary_card (create_time);

CREATE INDEX hrm_employee_training_experience_idx_employee_id ON hrm_employee_training_experience (employee_id);

CREATE INDEX hrm_employee_training_experience_idx_sort ON hrm_employee_training_experience (sort);

CREATE INDEX hrm_employee_training_experience_idx_create_time ON hrm_employee_training_experience (create_time);

CREATE INDEX hrm_employee_work_experience_idx_employee_id ON hrm_employee_work_experience (employee_id);

CREATE INDEX hrm_employee_work_experience_idx_sort ON hrm_employee_work_experience (sort);

CREATE INDEX hrm_employee_work_experience_idx_create_time ON hrm_employee_work_experience (create_time);

CREATE INDEX hrm_insurance_employee_info_idx_employee_id ON hrm_insurance_employee_info (employee_id);

CREATE INDEX hrm_insurance_employee_info_idx_scheme_id ON hrm_insurance_employee_info (scheme_id);

CREATE INDEX hrm_insurance_employee_info_idx_create_time ON hrm_insurance_employee_info (create_time);

CREATE INDEX hrm_insurance_month_employee_record_idx_month_record_id ON hrm_insurance_month_employee_record (month_record_id);

CREATE INDEX hrm_insurance_month_employee_record_idx_employee_id ON hrm_insurance_month_employee_record (employee_id);

CREATE INDEX hrm_insurance_month_employee_record_idx_scheme_id ON hrm_insurance_month_employee_record (scheme_id);

CREATE INDEX hrm_insurance_month_employee_record_idx_year ON hrm_insurance_month_employee_record (year);

CREATE INDEX hrm_insurance_month_employee_record_idx_create_time ON hrm_insurance_month_employee_record (create_time);

CREATE INDEX hrm_insurance_month_record_idx_year ON hrm_insurance_month_record (year);

CREATE INDEX hrm_insurance_month_record_idx_month ON hrm_insurance_month_record (month);

CREATE INDEX hrm_insurance_month_record_idx_create_time ON hrm_insurance_month_record (create_time);

CREATE INDEX hrm_insurance_scheme_idx_name ON hrm_insurance_scheme (name);

CREATE INDEX hrm_insurance_scheme_idx_area_id ON hrm_insurance_scheme (area_id);

CREATE INDEX hrm_insurance_scheme_idx_create_time ON hrm_insurance_scheme (create_time);

CREATE INDEX hrm_insurance_scheme_project_idx_scheme_id ON hrm_insurance_scheme_project (scheme_id);

CREATE INDEX hrm_insurance_scheme_project_idx_type ON hrm_insurance_scheme_project (type);

CREATE INDEX hrm_insurance_scheme_project_idx_create_time ON hrm_insurance_scheme_project (create_time);

CREATE INDEX hrm_performance_assessment_idx_status ON hrm_performance_assessment (status);

CREATE INDEX hrm_performance_assessment_idx_employee_id ON hrm_performance_assessment (employee_id);

CREATE INDEX hrm_performance_assessment_idx_plan_id ON hrm_performance_assessment (plan_id);

CREATE INDEX hrm_performance_assessment_idx_archive_time ON hrm_performance_assessment (archive_time);

CREATE INDEX hrm_performance_assessment_idx_create_time ON hrm_performance_assessment (create_time);

CREATE INDEX hrm_performance_assessment_action_record_idx_assessment_id ON hrm_performance_assessment_action_record (assessment_id);

CREATE INDEX hrm_performance_assessment_action_record_idx_create_time ON hrm_performance_assessment_action_record (create_time);

CREATE INDEX hrm_performance_assessment_appeal_record_idx_assessment_id ON hrm_performance_assessment_appeal_record (assessment_id);

CREATE INDEX hrm_performance_assessment_appeal_record_idx_status ON hrm_performance_assessment_appeal_record (status);

CREATE INDEX hrm_performance_assessment_appeal_record_idx_create_time ON hrm_performance_assessment_appeal_record (create_time);

CREATE INDEX hrm_performance_assessment_dimension_idx_assessment_id ON hrm_performance_assessment_dimension (assessment_id);

CREATE INDEX hrm_performance_assessment_dimension_idx_sort ON hrm_performance_assessment_dimension (sort);

CREATE INDEX hrm_performance_assessment_dimension_idx_create_time ON hrm_performance_assessment_dimension (create_time);

CREATE INDEX hrm_performance_assessment_quota_idx_assessment_id ON hrm_performance_assessment_quota (assessment_id);

CREATE INDEX hrm_performance_assessment_quota_idx_sort ON hrm_performance_assessment_quota (sort);

CREATE INDEX hrm_performance_assessment_quota_idx_create_time ON hrm_performance_assessment_quota (create_time);

CREATE INDEX hrm_performance_assessment_quota_score_idx_assessment_stage_id ON hrm_performance_assessment_quota_score (assessment_stage_id);

CREATE INDEX hrm_performance_assessment_quota_score_idx_create_time ON hrm_performance_assessment_quota_score (create_time);

CREATE INDEX hrm_performance_assessment_stage_idx_assessment_id ON hrm_performance_assessment_stage (assessment_id);

CREATE INDEX hrm_performance_assessment_stage_idx_type ON hrm_performance_assessment_stage (type);

CREATE INDEX hrm_performance_assessment_stage_idx_sort ON hrm_performance_assessment_stage (sort);

CREATE INDEX hrm_performance_assessment_stage_idx_status ON hrm_performance_assessment_stage (status);

CREATE INDEX hrm_performance_assessment_stage_idx_create_time ON hrm_performance_assessment_stage (create_time);

CREATE INDEX hrm_performance_assessment_template_idx_status ON hrm_performance_assessment_template (status);

CREATE INDEX hrm_performance_assessment_template_idx_update_time ON hrm_performance_assessment_template (update_time);

CREATE INDEX hrm_performance_assessment_template_idx_name ON hrm_performance_assessment_template (name);

CREATE INDEX hrm_performance_assessment_template_idx_create_time ON hrm_performance_assessment_template (create_time);

CREATE INDEX hrm_performance_plan_idx_name ON hrm_performance_plan (name);

CREATE INDEX hrm_performance_plan_idx_status ON hrm_performance_plan (status);

CREATE INDEX hrm_performance_plan_idx_paid_for_month ON hrm_performance_plan (paid_for_month);

CREATE INDEX hrm_performance_plan_idx_create_time ON hrm_performance_plan (create_time);

CREATE INDEX hrm_performance_result_template_idx_status ON hrm_performance_result_template (status);

CREATE INDEX hrm_performance_result_template_idx_update_time ON hrm_performance_result_template (update_time);

CREATE INDEX hrm_performance_result_template_idx_name ON hrm_performance_result_template (name);

CREATE INDEX hrm_performance_result_template_idx_create_time ON hrm_performance_result_template (create_time);

CREATE INDEX hrm_recruit_candidate_idx_status ON hrm_recruit_candidate (status);

CREATE INDEX hrm_recruit_candidate_idx_create_time ON hrm_recruit_candidate (create_time);

CREATE INDEX hrm_recruit_candidate_idx_status_update_time ON hrm_recruit_candidate (status_update_time);

CREATE INDEX hrm_recruit_candidate_idx_channel_id ON hrm_recruit_candidate (channel_id);

CREATE INDEX hrm_recruit_channel_idx_status ON hrm_recruit_channel (status);

CREATE INDEX hrm_recruit_channel_idx_sort ON hrm_recruit_channel (sort);

CREATE INDEX hrm_recruit_channel_idx_name ON hrm_recruit_channel (name);

CREATE INDEX hrm_recruit_channel_idx_create_time ON hrm_recruit_channel (create_time);

CREATE INDEX hrm_recruit_interview_idx_candidate_id ON hrm_recruit_interview (candidate_id);

CREATE INDEX hrm_recruit_interview_idx_stage_number ON hrm_recruit_interview (stage_number);

CREATE INDEX hrm_recruit_interview_idx_interview_time ON hrm_recruit_interview (interview_time);

CREATE INDEX hrm_recruit_interview_idx_result ON hrm_recruit_interview (result);

CREATE INDEX hrm_recruit_interview_idx_create_time ON hrm_recruit_interview (create_time);

CREATE INDEX hrm_recruit_post_idx_owner_employee_id ON hrm_recruit_post (owner_employee_id);

CREATE INDEX hrm_recruit_post_idx_status ON hrm_recruit_post (status);

CREATE INDEX hrm_recruit_post_idx_create_time ON hrm_recruit_post (create_time);

CREATE INDEX hrm_recruit_post_type_idx_status ON hrm_recruit_post_type (status);

CREATE INDEX hrm_recruit_post_type_idx_sort ON hrm_recruit_post_type (sort);

CREATE INDEX hrm_recruit_post_type_idx_create_time ON hrm_recruit_post_type (create_time);

CREATE INDEX hrm_salary_change_record_idx_status ON hrm_salary_change_record (status);

CREATE INDEX hrm_salary_change_record_idx_employee_id ON hrm_salary_change_record (employee_id);

CREATE INDEX hrm_salary_change_record_idx_effect_time ON hrm_salary_change_record (effect_time);

CREATE INDEX hrm_salary_change_record_idx_type ON hrm_salary_change_record (type);

CREATE INDEX hrm_salary_change_record_idx_create_time ON hrm_salary_change_record (create_time);

CREATE INDEX hrm_salary_change_template_idx_create_time ON hrm_salary_change_template (create_time);

CREATE INDEX hrm_salary_config_idx_create_time ON hrm_salary_config (create_time);

CREATE INDEX hrm_salary_employee_info_idx_employee_id ON hrm_salary_employee_info (employee_id);

CREATE INDEX hrm_salary_employee_info_idx_change_type ON hrm_salary_employee_info (change_type);

CREATE INDEX hrm_salary_employee_info_idx_create_time ON hrm_salary_employee_info (create_time);

CREATE INDEX hrm_salary_group_idx_tax_rule_id ON hrm_salary_group (tax_rule_id);

CREATE INDEX hrm_salary_group_idx_name ON hrm_salary_group (name);

CREATE INDEX hrm_salary_group_idx_create_time ON hrm_salary_group (create_time);

CREATE INDEX hrm_salary_month_employee_record_idx_employee_id ON hrm_salary_month_employee_record (employee_id);

CREATE INDEX hrm_salary_month_employee_record_idx_month_record_id ON hrm_salary_month_employee_record (month_record_id);

CREATE INDEX hrm_salary_month_employee_record_idx_year ON hrm_salary_month_employee_record (year);

CREATE INDEX hrm_salary_month_employee_record_idx_month ON hrm_salary_month_employee_record (month);

CREATE INDEX hrm_salary_month_employee_record_idx_create_time ON hrm_salary_month_employee_record (create_time);

CREATE INDEX hrm_salary_month_record_idx_year ON hrm_salary_month_record (year);

CREATE INDEX hrm_salary_month_record_idx_month ON hrm_salary_month_record (month);

CREATE INDEX hrm_salary_month_record_idx_status ON hrm_salary_month_record (status);

CREATE INDEX hrm_salary_month_record_idx_create_time ON hrm_salary_month_record (create_time);

CREATE INDEX hrm_salary_option_idx_code ON hrm_salary_option (code);

CREATE INDEX hrm_salary_option_idx_enabled ON hrm_salary_option (enabled);

CREATE INDEX hrm_salary_option_idx_visible ON hrm_salary_option (visible);

CREATE INDEX hrm_salary_option_idx_create_time ON hrm_salary_option (create_time);

CREATE INDEX hrm_salary_option_template_idx_code ON hrm_salary_option_template (code);

CREATE INDEX hrm_salary_option_template_idx_create_time ON hrm_salary_option_template (create_time);

CREATE INDEX hrm_salary_slip_idx_employee_id ON hrm_salary_slip (employee_id);

CREATE INDEX hrm_salary_slip_idx_send_record_id ON hrm_salary_slip (send_record_id);

CREATE INDEX hrm_salary_slip_idx_read_status ON hrm_salary_slip (read_status);

CREATE INDEX hrm_salary_slip_idx_month_employee_record_id ON hrm_salary_slip (month_employee_record_id);

CREATE INDEX hrm_salary_slip_idx_create_time ON hrm_salary_slip (create_time);

CREATE INDEX hrm_salary_slip_send_record_idx_year ON hrm_salary_slip_send_record (year);

CREATE INDEX hrm_salary_slip_send_record_idx_month ON hrm_salary_slip_send_record (month);

CREATE INDEX hrm_salary_slip_send_record_idx_create_time ON hrm_salary_slip_send_record (create_time);

CREATE INDEX hrm_salary_slip_template_idx_default_status ON hrm_salary_slip_template (default_status);

CREATE INDEX hrm_salary_slip_template_idx_create_time ON hrm_salary_slip_template (create_time);

CREATE INDEX hrm_salary_tax_rule_idx_create_time ON hrm_salary_tax_rule (create_time);

CREATE INDEX milestones_idx_user_date ON milestones (user_id,milestone_date);

CREATE UNIQUE INDEX notifications_uk_user_dedup ON notifications (user_id,dedup_key);

CREATE INDEX notifications_idx_user_read ON notifications (user_id,read_flag);

CREATE INDEX notifications_idx_user_created ON notifications (user_id,created_at DESC);

CREATE INDEX oa_announcement_idx_type ON oa_announcement (type);

CREATE INDEX oa_announcement_idx_priority ON oa_announcement (priority);

CREATE INDEX oa_announcement_idx_create_time ON oa_announcement (create_time);

CREATE INDEX oa_announcement_receiver_idx_announcement_id ON oa_announcement_receiver (announcement_id);

CREATE INDEX oa_announcement_receiver_idx_receiver_user_id ON oa_announcement_receiver (receiver_user_id);

CREATE INDEX oa_attendance_idx_user_id ON oa_attendance (user_id);

CREATE INDEX oa_attendance_idx_attendance_time ON oa_attendance (attendance_time);

CREATE UNIQUE INDEX oa_leave_apply_uk_wecom_request_id ON oa_leave_apply (wecom_request_id);

CREATE INDEX oa_leave_apply_idx_creator ON oa_leave_apply (creator);

CREATE INDEX oa_leave_apply_idx_status ON oa_leave_apply (status);

CREATE INDEX oa_leave_apply_idx_process_instance_id ON oa_leave_apply (process_instance_id);

CREATE INDEX oa_meeting_room_idx_status ON oa_meeting_room (status);

CREATE INDEX oa_meeting_room_idx_type ON oa_meeting_room (type);

CREATE INDEX oa_meeting_room_booking_idx_room_id_start_time ON oa_meeting_room_booking (room_id,start_time);

CREATE INDEX oa_meeting_room_booking_idx_status ON oa_meeting_room_booking (status);

CREATE INDEX oa_meeting_room_booking_idx_use_status ON oa_meeting_room_booking (use_status);

CREATE INDEX oa_official_doc_receive_idx_send_id ON oa_official_doc_receive (send_id);

CREATE INDEX oa_official_doc_receive_idx_receive_dept_id ON oa_official_doc_receive (receive_dept_id);

CREATE INDEX oa_official_doc_receive_idx_handle_status ON oa_official_doc_receive (handle_status);

CREATE INDEX oa_official_doc_send_idx_document_no ON oa_official_doc_send (document_no);

CREATE INDEX oa_official_doc_send_idx_status ON oa_official_doc_send (status);

CREATE INDEX oa_official_doc_send_idx_creator ON oa_official_doc_send (creator);

CREATE INDEX oa_official_doc_template_idx_status ON oa_official_doc_template (status);

CREATE INDEX oa_overtime_apply_idx_creator ON oa_overtime_apply (creator);

CREATE INDEX oa_overtime_apply_idx_status ON oa_overtime_apply (status);

CREATE INDEX oa_regular_apply_idx_creator ON oa_regular_apply (creator);

CREATE INDEX oa_regular_apply_idx_status ON oa_regular_apply (status);

CREATE INDEX oa_reimbursement_idx_creator ON oa_reimbursement (creator);

CREATE INDEX oa_reimbursement_idx_status ON oa_reimbursement (status);

CREATE INDEX oa_reimbursement_idx_process_instance_id ON oa_reimbursement (process_instance_id);

CREATE INDEX oa_resign_apply_idx_creator ON oa_resign_apply (creator);

CREATE INDEX oa_resign_apply_idx_status ON oa_resign_apply (status);

CREATE INDEX oa_schedule_idx_creator ON oa_schedule (creator);

CREATE INDEX oa_schedule_idx_start_time ON oa_schedule (start_time);

CREATE INDEX oa_schedule_participant_idx_schedule_id ON oa_schedule_participant (schedule_id);

CREATE INDEX oa_schedule_participant_idx_user_id ON oa_schedule_participant (user_id);

CREATE INDEX oa_seal_idx_dept_id ON oa_seal (dept_id);

CREATE INDEX oa_seal_idx_keeper_user_id ON oa_seal (keeper_user_id);

CREATE INDEX oa_seal_idx_status ON oa_seal (status);

CREATE INDEX oa_seal_apply_idx_seal_id ON oa_seal_apply (seal_id);

CREATE INDEX oa_seal_apply_idx_user_id ON oa_seal_apply (user_id);

CREATE INDEX oa_seal_apply_idx_status ON oa_seal_apply (status);

CREATE INDEX oa_supply_apply_idx_creator ON oa_supply_apply (creator);

CREATE INDEX oa_supply_apply_idx_dept_id ON oa_supply_apply (dept_id);

CREATE INDEX oa_supply_apply_idx_status ON oa_supply_apply (status);

CREATE INDEX oa_supply_apply_item_idx_apply_id ON oa_supply_apply_item (apply_id);

CREATE INDEX oa_supply_apply_item_idx_item_id ON oa_supply_apply_item (item_id);

CREATE INDEX oa_supply_apply_item_idx_status ON oa_supply_apply_item (status);

CREATE INDEX oa_supply_item_idx_dept_id ON oa_supply_item (dept_id);

CREATE INDEX oa_supply_item_idx_category ON oa_supply_item (category);

CREATE INDEX oa_supply_item_idx_status ON oa_supply_item (status);

CREATE INDEX oa_task_idx_creator ON oa_task (creator);

CREATE INDEX oa_task_idx_status ON oa_task (status);

CREATE INDEX oa_task_idx_type ON oa_task (type);

CREATE INDEX oa_task_log_idx_task_id ON oa_task_log (task_id);

CREATE INDEX oa_task_log_idx_user_id ON oa_task_log (user_id);

CREATE INDEX oa_task_receiver_idx_task_id_user_id ON oa_task_receiver (task_id,user_id);

CREATE INDEX oa_task_receiver_idx_user_id ON oa_task_receiver (user_id);

CREATE INDEX oa_travel_apply_idx_creator ON oa_travel_apply (creator);

CREATE INDEX oa_travel_apply_idx_dept_id ON oa_travel_apply (dept_id);

CREATE INDEX oa_travel_apply_idx_status ON oa_travel_apply (status);

CREATE INDEX oa_travel_reimbursement_idx_travel_apply_id ON oa_travel_reimbursement (travel_apply_id);

CREATE INDEX oa_travel_reimbursement_idx_creator ON oa_travel_reimbursement (creator);

CREATE INDEX oa_travel_reimbursement_idx_dept_id ON oa_travel_reimbursement (dept_id);

CREATE INDEX oa_vehicle_idx_dept_id ON oa_vehicle (dept_id);

CREATE INDEX oa_vehicle_idx_status ON oa_vehicle (status);

CREATE INDEX oa_vehicle_apply_idx_vehicle_id ON oa_vehicle_apply (vehicle_id);

CREATE INDEX oa_vehicle_apply_idx_user_id ON oa_vehicle_apply (user_id);

CREATE INDEX oa_vehicle_apply_idx_status ON oa_vehicle_apply (status);

CREATE INDEX oa_vehicle_return_idx_apply_id ON oa_vehicle_return (apply_id);

CREATE INDEX oa_vehicle_return_idx_vehicle_id ON oa_vehicle_return (vehicle_id);

CREATE INDEX oa_vehicle_return_idx_status ON oa_vehicle_return (status);

CREATE INDEX oa_work_report_idx_creator ON oa_work_report (creator);

CREATE INDEX oa_work_report_idx_dept_id ON oa_work_report (dept_id);

CREATE INDEX oa_work_report_idx_start_time ON oa_work_report (start_time);

CREATE INDEX oa_work_report_comment_idx_report ON oa_work_report_comment (report_id);

CREATE INDEX oa_work_report_comment_idx_reviewer ON oa_work_report_comment (reviewer_id);

CREATE UNIQUE INDEX portal_categories_uq_categories_scope_name_active ON portal_categories (scope,active_name);

CREATE UNIQUE INDEX portal_categories_uq_categories_scope_code ON portal_categories (scope,code);

CREATE INDEX portal_categories_ix_categories_scope_archived_sort ON portal_categories (scope,archived_at,sort_order);

CREATE INDEX portal_categories_ix_categories_parent ON portal_categories (parent_id);

CREATE INDEX portal_news_ix_news_status_published ON portal_news (status,published_at);

CREATE INDEX portal_news_ix_news_author ON portal_news (author_username);

CREATE INDEX portal_news_comment_ix_news_comments_news_id ON portal_news_comment (news_id);

CREATE INDEX portal_news_comment_ix_news_comments_created_at ON portal_news_comment (created_at);

CREATE INDEX portal_news_comment_ix_news_comments_parent ON portal_news_comment (parent_id);

CREATE UNIQUE INDEX portal_news_like_uq_news_likes_news_user ON portal_news_like (news_id,username);

CREATE INDEX portal_news_like_ix_news_likes_news_id ON portal_news_like (news_id);

CREATE INDEX portal_page_views_ix_page_views_page_url ON portal_page_views (page_url);

CREATE INDEX portal_page_views_ix_page_views_username ON portal_page_views (username);

CREATE INDEX portal_page_views_ix_page_views_viewed_at ON portal_page_views (viewed_at);

CREATE INDEX portal_wiki_ix_wiki_status ON portal_wiki (status);

CREATE INDEX portal_wiki_ix_wiki_author ON portal_wiki (author_username);

CREATE INDEX recognitions_idx_to ON recognitions (to_id);

CREATE UNIQUE INDEX rhythm_node_follow_uk_user_node ON rhythm_node_follow (user_id,node_id);

CREATE INDEX rhythm_nodes_idx_range ON rhythm_nodes (start_date,end_date);

CREATE INDEX sys_dept_idx_sys_dept_parent ON sys_dept (parent_id);

CREATE UNIQUE INDEX sys_dict_data_uk_sys_dict_data ON sys_dict_data (dict_type,value);

CREATE UNIQUE INDEX sys_dict_type_uk_sys_dict_type ON sys_dict_type (type);

CREATE INDEX sys_directory_sync_log_idx_sys_directory_sync_log_created ON sys_directory_sync_log (created_at);

CREATE INDEX sys_employee_sync_job_idx_sync_job_status ON sys_employee_sync_job (status,lease_expires_at);

CREATE INDEX sys_employee_sync_job_idx_sync_job_created ON sys_employee_sync_job (created_at);

CREATE INDEX sys_menu_idx_sys_menu_permission ON sys_menu (permission);

CREATE INDEX sys_operate_log_idx_operate_log_user ON sys_operate_log (user_id,created_at);

CREATE INDEX sys_operate_log_idx_operate_log_biz ON sys_operate_log (target_table,biz_id);

CREATE INDEX sys_operate_log_idx_operate_log_created ON sys_operate_log (created_at);

CREATE UNIQUE INDEX sys_role_uk_sys_role_code ON sys_role (code);

CREATE UNIQUE INDEX sys_role_menu_uk_sys_role_menu ON sys_role_menu (role_id,menu_id);

CREATE UNIQUE INDEX sys_user_uk_sys_user_username ON sys_user (username);

CREATE UNIQUE INDEX sys_user_uk_sys_user_wecom ON sys_user (wecom_userid);

CREATE INDEX sys_user_idx_sys_user_main_dept ON sys_user (main_dept_id);

CREATE UNIQUE INDEX sys_user_dept_uk_sys_user_dept ON sys_user_dept (user_id,dept_id);

CREATE INDEX sys_user_dept_idx_sys_user_dept_dept ON sys_user_dept (dept_id);

CREATE UNIQUE INDEX sys_user_role_uk_sys_user_role ON sys_user_role (user_id,role_id);

CREATE INDEX sys_user_role_idx_sys_user_role_role ON sys_user_role (role_id);

CREATE INDEX voices_idx_user_created ON voices (user_id,created_at DESC);

CREATE UNIQUE INDEX portal_ai_models_uk_ai_model ON portal_ai_models (provider,model_id);

CREATE UNIQUE INDEX portal_ai_task_setting_versions_uk_ai_task_version ON portal_ai_task_setting_versions (task_name,version_no);

CREATE INDEX portal_esports_bookings_ix_esports_bookings_date_status ON portal_esports_bookings (booking_date, status);

CREATE INDEX portal_esports_bookings_ix_esports_bookings_applicant_date ON portal_esports_bookings (applicant_user_id, booking_date);

CREATE INDEX portal_esports_bookings_ix_esports_bookings_approver_date ON portal_esports_bookings (approver_user_id, booking_date);

CREATE UNIQUE INDEX portal_esports_approval_steps_uq_esports_approval_step_person ON portal_esports_approval_steps (booking_id, stage_index, approver_user_id);

CREATE INDEX portal_esports_approval_steps_ix_esports_approval_steps_approve ON portal_esports_approval_steps (approver_user_id, status, booking_id);

CREATE INDEX portal_esports_booking_logs_ix_esports_booking_logs_booking_cre ON portal_esports_booking_logs (booking_id, created_at, id);

CREATE INDEX portal_events_ix_events_start_date ON portal_events (start_date);

CREATE INDEX portal_products_ix_products_launch_date ON portal_products (launch_date);

CREATE INDEX portal_feedback_ix_feedback_created ON portal_feedback (created_at);

CREATE INDEX portal_feedback_ix_feedback_status ON portal_feedback (status);

CREATE INDEX portal_hr_layout_jobs_ix_hr_layout_owner_created ON portal_hr_layout_jobs (user_id,created_at);

CREATE INDEX portal_hr_layout_jobs_ix_hr_layout_status ON portal_hr_layout_jobs (status);

CREATE INDEX portal_hr_layout_attempts_ix_hr_layout_rate ON portal_hr_layout_attempts (user_id,created_at);

CREATE UNIQUE INDEX portal_hr_documents_uq_hr_documents_slug ON portal_hr_documents (slug);

CREATE INDEX portal_hr_documents_ix_hr_documents_category ON portal_hr_documents (category);

CREATE INDEX portal_hr_documents_ix_hr_documents_status ON portal_hr_documents (status);

CREATE UNIQUE INDEX portal_hr_document_aliases_uq_hr_document_aliases_alias ON portal_hr_document_aliases (alias);

CREATE INDEX portal_hr_document_aliases_ix_hr_document_aliases_document_id ON portal_hr_document_aliases (document_id);

CREATE UNIQUE INDEX portal_hr_document_entry_bindings_uq_hr_document_entry_bindings ON portal_hr_document_entry_bindings (entry_key);

CREATE INDEX portal_hr_document_entry_bindings_ix_hr_document_entry_bindings ON portal_hr_document_entry_bindings (document_id);

CREATE UNIQUE INDEX portal_hr_document_versions_uq_hr_doc_versions_doc_ver ON portal_hr_document_versions (document_id, version);

CREATE UNIQUE INDEX portal_hr_document_versions_uq_hr_doc_versions_slug_ver ON portal_hr_document_versions (slug, version);

CREATE INDEX portal_hr_document_versions_ix_hr_doc_versions_document_id ON portal_hr_document_versions (document_id);

CREATE INDEX portal_hr_document_draft_versions_ix_hr_doc_draft_versions_doc_ ON portal_hr_document_draft_versions (document_id, created_at);

CREATE INDEX portal_content_push_jobs_ix_content_push_queue ON portal_content_push_jobs (status,id);

CREATE INDEX portal_content_push_jobs_ix_content_push_target ON portal_content_push_jobs (push_trigger,target_key,id);

CREATE UNIQUE INDEX portal_brand_docs_uq_brand_docs_brand_type ON portal_brand_docs (brand_code,doc_type);

CREATE INDEX portal_brand_doc_versions_ix_brand_doc_versions_doc ON portal_brand_doc_versions (doc_id);

CREATE UNIQUE INDEX portal_contract_entities_uq_contract_entities_name ON portal_contract_entities (name);

CREATE UNIQUE INDEX portal_resume_interview_processes_uq_resume_interview_active ON portal_resume_interview_processes (active_resume_id);

CREATE INDEX portal_resume_interview_processes_ix_resume_interview_resume_cr ON portal_resume_interview_processes (resume_id,created_at,id);

CREATE UNIQUE INDEX portal_resume_interview_rounds_uq_resume_interview_round_sequen ON portal_resume_interview_rounds (process_id,sequence);

CREATE UNIQUE INDEX portal_celebration_runs_uq_celebration_runs_date_kind ON portal_celebration_runs (run_date,kind);

CREATE UNIQUE INDEX portal_celebration_push_log_uq_celebration_source_log ON portal_celebration_push_log (source_log_id);

CREATE INDEX portal_celebration_push_log_ix_celebration_log_created ON portal_celebration_push_log (created_at);

CREATE INDEX portal_wecom_push_log_ix_wecom_push_trigger ON portal_wecom_push_log (trigger,target_key,pushed_at);

CREATE UNIQUE INDEX portal_celebration_cards_uq_celebration_card_token ON portal_celebration_cards (token);

CREATE INDEX portal_resumes_ix_resumes_status_created ON portal_resumes (status,created_at);

CREATE INDEX portal_resumes_ix_resumes_phone ON portal_resumes (phone);

CREATE INDEX portal_resumes_ix_resumes_email ON portal_resumes (email);

CREATE INDEX portal_resumes_ix_resumes_sha256 ON portal_resumes (file_sha256);

CREATE UNIQUE INDEX portal_material_products_uq_material_product_name ON portal_material_products (kind,active_name,active_brand);

CREATE UNIQUE INDEX portal_material_products_uq_material_product_kind ON portal_material_products (id,kind);

CREATE INDEX portal_materials_idx_material_product ON portal_materials (product_id);

CREATE INDEX portal_materials_idx_material_kind_site ON portal_materials (kind,site);

CREATE UNIQUE INDEX portal_materials_uq_material_variant ON portal_materials (product_id,site,active_variant);

CREATE UNIQUE INDEX portal_material_applies_uk_material_apply_no ON portal_material_applies (apply_no);

CREATE INDEX portal_material_apply_items_idx_material_apply ON portal_material_apply_items (apply_id);

CREATE INDEX portal_material_apply_items_idx_material_reference ON portal_material_apply_items (material_id);

CREATE UNIQUE INDEX portal_handbook_signoff_uq_handbook_signoff_user_doc_ver ON portal_handbook_signoff (user_id, document, version);

CREATE INDEX portal_handbook_signoff_ix_handbook_signoff_document_id ON portal_handbook_signoff (document_id);

CREATE UNIQUE INDEX portal_training_uq_training_source_prefix ON portal_training (source_prefix);

CREATE INDEX portal_training_ix_training_status ON portal_training (status);

CREATE INDEX portal_training_ix_training_category_id ON portal_training (category_id);

CREATE UNIQUE INDEX portal_training_lessons_uq_training_lessons_source_prefix ON portal_training_lessons (source_prefix);

CREATE INDEX portal_training_lessons_ix_training_lessons_course_sort ON portal_training_lessons (course_id, sort_order);

CREATE UNIQUE INDEX portal_training_lesson_progress_uq_training_lesson_progress_use ON portal_training_lesson_progress (username, lesson_id);

CREATE INDEX portal_training_progress_ix_training_progress_username ON portal_training_progress (username);

CREATE UNIQUE INDEX hrm_contract_template_uk_name_version ON hrm_contract_template (name, version);

CREATE UNIQUE INDEX hrm_employment_document_uk_request ON hrm_employment_document (request_key);

CREATE INDEX hrm_employment_document_idx_employee ON hrm_employment_document (employee_id);

CREATE INDEX hrm_employment_document_idx_disable ON hrm_employment_document (kind, account_disabled, effective_date);

CREATE INDEX hrm_candidate_attachment_idx_candidate ON hrm_candidate_attachment (candidate_id);

CREATE UNIQUE INDEX portal_workspace_records_uk_workspace_record_key ON portal_workspace_records (kind,record_key);

CREATE INDEX portal_workspace_records_idx_workspace_owner ON portal_workspace_records (kind,owner_id,status,id);

CREATE INDEX portal_workspace_records_idx_workspace_parent ON portal_workspace_records (kind,parent_id);

CREATE INDEX portal_workspace_records_idx_workspace_dept ON portal_workspace_records (kind,dept_id,status);

CREATE UNIQUE INDEX portal_workspace_answers_uk_workspace_survey_answer ON portal_workspace_answers (survey_id,user_id);

CREATE UNIQUE INDEX portal_workspace_approval_steps_uk_workspace_request_step ON portal_workspace_approval_steps (request_id,step_no);

CREATE INDEX portal_workspace_approval_steps_idx_workspace_approver ON portal_workspace_approval_steps (approver_id,status,request_id);

CREATE UNIQUE INDEX hrm_wecom_roster_sync_run_uk_roster_sync_active ON hrm_wecom_roster_sync_run (active_slot);

CREATE INDEX hrm_wecom_roster_sync_run_idx_roster_sync_created ON hrm_wecom_roster_sync_run (created_at);

CREATE UNIQUE INDEX oa_wecom_approval_link_uk_type_biz ON oa_wecom_approval_link (apply_type, biz_id);

CREATE UNIQUE INDEX oa_wecom_approval_link_uk_request_id ON oa_wecom_approval_link (request_id);

-- Preserve account identity uniqueness across username/userid letter case.

CREATE UNIQUE INDEX sys_user_username_lower_uq ON sys_user (LOWER(username));

CREATE UNIQUE INDEX sys_user_wecom_userid_lower_uq ON sys_user (LOWER(wecom_userid));

CREATE UNIQUE INDEX portal_categories_scope_code_lower_uq ON portal_categories (scope,LOWER(code));

ALTER TABLE portal_categories ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES portal_categories (id) ON DELETE RESTRICT;

ALTER TABLE portal_news_comment ADD CONSTRAINT fk_news_comment_news FOREIGN KEY (news_id) REFERENCES portal_news (id) ON DELETE CASCADE;

ALTER TABLE portal_news_comment ADD CONSTRAINT fk_news_comment_parent FOREIGN KEY (parent_id) REFERENCES portal_news_comment (id) ON DELETE SET NULL;

ALTER TABLE portal_news_like ADD CONSTRAINT fk_news_like_news FOREIGN KEY (news_id) REFERENCES portal_news (id) ON DELETE CASCADE;

ALTER TABLE portal_esports_approval_steps ADD CONSTRAINT fk_esports_approval_steps_booking FOREIGN KEY (booking_id) REFERENCES portal_esports_bookings (id) ON DELETE CASCADE;

ALTER TABLE portal_esports_booking_logs ADD CONSTRAINT fk_esports_booking_logs_booking FOREIGN KEY (booking_id) REFERENCES portal_esports_bookings (id) ON DELETE CASCADE;

ALTER TABLE portal_hr_document_aliases ADD CONSTRAINT fk_hr_document_aliases_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT;

ALTER TABLE portal_hr_document_entry_bindings ADD CONSTRAINT fk_hr_document_entry_bindings_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT;

ALTER TABLE portal_hr_document_versions ADD CONSTRAINT fk_hr_doc_versions_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT;

ALTER TABLE portal_hr_document_draft_versions ADD CONSTRAINT fk_hr_doc_draft_versions_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT;

ALTER TABLE portal_brand_doc_versions ADD FOREIGN KEY(doc_id) REFERENCES portal_brand_docs(id) ON DELETE CASCADE;

ALTER TABLE portal_resume_interview_rounds ADD FOREIGN KEY(process_id) REFERENCES portal_resume_interview_processes(id) ON DELETE CASCADE;

ALTER TABLE portal_handbook_signoff ADD CONSTRAINT fk_handbook_signoff_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT;

ALTER TABLE portal_training ADD CONSTRAINT fk_training_category FOREIGN KEY (category_id) REFERENCES portal_categories (id) ON DELETE RESTRICT;

ALTER TABLE portal_training_lessons ADD CONSTRAINT fk_training_lessons_course FOREIGN KEY (course_id) REFERENCES portal_training (id) ON DELETE CASCADE;

ALTER TABLE portal_training_lesson_progress ADD CONSTRAINT fk_training_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES portal_training_lessons (id) ON DELETE CASCADE;

ALTER TABLE portal_material_products ADD CONSTRAINT fk_oa_material_category FOREIGN KEY(category_id) REFERENCES portal_categories(id) ON DELETE RESTRICT;

ALTER TABLE portal_materials ADD CONSTRAINT fk_oa_material_product FOREIGN KEY(product_id,kind) REFERENCES portal_material_products(id,kind) ON DELETE RESTRICT;

ALTER TABLE portal_materials ADD CONSTRAINT fk_oa_material_owner FOREIGN KEY(site,kind) REFERENCES portal_material_site_owners(site,kind) ON DELETE RESTRICT;

ALTER TABLE portal_material_apply_items ADD CONSTRAINT fk_oa_material_apply FOREIGN KEY(apply_id) REFERENCES portal_material_applies(id) ON DELETE CASCADE;

ALTER TABLE portal_material_apply_items ADD CONSTRAINT fk_oa_material_item FOREIGN KEY(material_id) REFERENCES portal_materials(id) ON DELETE RESTRICT;

ALTER TABLE portal_material_apply_logs ADD CONSTRAINT fk_oa_material_log FOREIGN KEY(apply_id) REFERENCES portal_material_applies(id) ON DELETE CASCADE;

COMMENT ON COLUMN flow_definition.id IS '主键id';

COMMENT ON COLUMN flow_definition.flow_code IS '流程编码';

COMMENT ON COLUMN flow_definition.flow_name IS '流程名称';

COMMENT ON COLUMN flow_definition.model_value IS '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）';

COMMENT ON COLUMN flow_definition.category IS '流程类别';

COMMENT ON COLUMN flow_definition.version IS '流程版本';

COMMENT ON COLUMN flow_definition.is_publish IS '是否发布（0未发布 1已发布 9失效）';

COMMENT ON COLUMN flow_definition.form_custom IS '审批表单是否自定义（Y是 N否）';

COMMENT ON COLUMN flow_definition.form_path IS '审批表单路径';

COMMENT ON COLUMN flow_definition.activity_status IS '流程激活状态（0挂起 1激活）';

COMMENT ON COLUMN flow_definition.listener_type IS '监听器类型';

COMMENT ON COLUMN flow_definition.listener_path IS '监听器路径';

COMMENT ON COLUMN flow_definition.ext IS '业务详情 存业务表对象json字符串';

COMMENT ON COLUMN flow_definition.create_time IS '创建时间';

COMMENT ON COLUMN flow_definition.create_by IS '创建人';

COMMENT ON COLUMN flow_definition.update_time IS '更新时间';

COMMENT ON COLUMN flow_definition.update_by IS '更新人';

COMMENT ON COLUMN flow_definition.del_flag IS '删除标志';

COMMENT ON COLUMN flow_definition.tenant_id IS '租户id';

COMMENT ON TABLE flow_definition IS '流程定义表';

COMMENT ON COLUMN flow_form.id IS '主键id';

COMMENT ON COLUMN flow_form.form_code IS '表单编码';

COMMENT ON COLUMN flow_form.form_name IS '表单名称';

COMMENT ON COLUMN flow_form.version IS '表单版本';

COMMENT ON COLUMN flow_form.is_publish IS '是否发布（0未发布 1已发布 9失效）';

COMMENT ON COLUMN flow_form.form_type IS '表单类型（0内置表单 存 form_content 1外挂表单 存form_path）';

COMMENT ON COLUMN flow_form.form_path IS '表单路径';

COMMENT ON COLUMN flow_form.form_content IS '表单内容';

COMMENT ON COLUMN flow_form.ext IS '表单扩展，用户自行使用';

COMMENT ON COLUMN flow_form.create_time IS '创建时间';

COMMENT ON COLUMN flow_form.create_by IS '创建人';

COMMENT ON COLUMN flow_form.update_time IS '更新时间';

COMMENT ON COLUMN flow_form.update_by IS '更新人';

COMMENT ON COLUMN flow_form.del_flag IS '删除标志';

COMMENT ON COLUMN flow_form.tenant_id IS '租户id';

COMMENT ON TABLE flow_form IS '流程表单表';

COMMENT ON COLUMN flow_his_task.id IS '主键id';

COMMENT ON COLUMN flow_his_task.definition_id IS '对应flow_definition表的id';

COMMENT ON COLUMN flow_his_task.instance_id IS '对应flow_instance表的id';

COMMENT ON COLUMN flow_his_task.task_id IS '对应flow_task表的id';

COMMENT ON COLUMN flow_his_task.node_code IS '开始节点编码';

COMMENT ON COLUMN flow_his_task.node_name IS '开始节点名称';

COMMENT ON COLUMN flow_his_task.node_type IS '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_his_task.target_node_code IS '目标节点编码';

COMMENT ON COLUMN flow_his_task.target_node_name IS '结束节点名称';

COMMENT ON COLUMN flow_his_task.approver IS '审批人';

COMMENT ON COLUMN flow_his_task.cooperate_type IS '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)';

COMMENT ON COLUMN flow_his_task.collaborator IS '协作人';

COMMENT ON COLUMN flow_his_task.skip_type IS '流转类型（PASS通过 REJECT退回 NONE无动作）';

COMMENT ON COLUMN flow_his_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';

COMMENT ON COLUMN flow_his_task.form_custom IS '审批表单是否自定义（Y是 N否）';

COMMENT ON COLUMN flow_his_task.form_path IS '审批表单路径';

COMMENT ON COLUMN flow_his_task.message IS '审批意见';

COMMENT ON COLUMN flow_his_task.variable IS '任务变量';

COMMENT ON COLUMN flow_his_task.ext IS '业务详情 存业务表对象json字符串';

COMMENT ON COLUMN flow_his_task.create_time IS '任务开始时间';

COMMENT ON COLUMN flow_his_task.update_time IS '审批完成时间';

COMMENT ON COLUMN flow_his_task.del_flag IS '删除标志';

COMMENT ON COLUMN flow_his_task.tenant_id IS '租户id';

COMMENT ON TABLE flow_his_task IS '历史任务记录表';

COMMENT ON COLUMN flow_instance.id IS '主键id';

COMMENT ON COLUMN flow_instance.definition_id IS '对应flow_definition表的id';

COMMENT ON COLUMN flow_instance.business_id IS '业务id';

COMMENT ON COLUMN flow_instance.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_instance.node_code IS '流程节点编码';

COMMENT ON COLUMN flow_instance.node_name IS '流程节点名称';

COMMENT ON COLUMN flow_instance.variable IS '任务变量';

COMMENT ON COLUMN flow_instance.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';

COMMENT ON COLUMN flow_instance.activity_status IS '流程激活状态（0挂起 1激活）';

COMMENT ON COLUMN flow_instance.def_json IS '流程定义json';

COMMENT ON COLUMN flow_instance.create_time IS '创建时间';

COMMENT ON COLUMN flow_instance.create_by IS '创建人';

COMMENT ON COLUMN flow_instance.update_time IS '更新时间';

COMMENT ON COLUMN flow_instance.update_by IS '更新人';

COMMENT ON COLUMN flow_instance.ext IS '扩展字段，预留给业务系统使用';

COMMENT ON COLUMN flow_instance.del_flag IS '删除标志';

COMMENT ON COLUMN flow_instance.tenant_id IS '租户id';

COMMENT ON TABLE flow_instance IS '流程实例表';

COMMENT ON COLUMN flow_node.id IS '主键id';

COMMENT ON COLUMN flow_node.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_node.definition_id IS '流程定义id';

COMMENT ON COLUMN flow_node.node_code IS '流程节点编码';

COMMENT ON COLUMN flow_node.node_name IS '流程节点名称';

COMMENT ON COLUMN flow_node.permission_flag IS '权限标识（权限类型:权限标识，可以多个，用@@隔开)';

COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';

COMMENT ON COLUMN flow_node.coordinate IS '坐标';

COMMENT ON COLUMN flow_node.any_node_skip IS '任意结点跳转';

COMMENT ON COLUMN flow_node.listener_type IS '监听器类型';

COMMENT ON COLUMN flow_node.listener_path IS '监听器路径';

COMMENT ON COLUMN flow_node.form_custom IS '审批表单是否自定义（Y是 N否）';

COMMENT ON COLUMN flow_node.form_path IS '审批表单路径';

COMMENT ON COLUMN flow_node.version IS '版本';

COMMENT ON COLUMN flow_node.create_time IS '创建时间';

COMMENT ON COLUMN flow_node.create_by IS '创建人';

COMMENT ON COLUMN flow_node.update_time IS '更新时间';

COMMENT ON COLUMN flow_node.update_by IS '更新人';

COMMENT ON COLUMN flow_node.ext IS '节点扩展属性';

COMMENT ON COLUMN flow_node.del_flag IS '删除标志';

COMMENT ON COLUMN flow_node.tenant_id IS '租户id';

COMMENT ON TABLE flow_node IS '流程节点表';

COMMENT ON COLUMN flow_skip.id IS '主键id';

COMMENT ON COLUMN flow_skip.definition_id IS '流程定义id';

COMMENT ON COLUMN flow_skip.now_node_code IS '当前流程节点的编码';

COMMENT ON COLUMN flow_skip.now_node_type IS '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_skip.next_node_code IS '下一个流程节点的编码';

COMMENT ON COLUMN flow_skip.next_node_type IS '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_skip.skip_name IS '跳转名称';

COMMENT ON COLUMN flow_skip.skip_type IS '跳转类型（PASS审批通过 REJECT退回）';

COMMENT ON COLUMN flow_skip.skip_condition IS '跳转条件';

COMMENT ON COLUMN flow_skip.coordinate IS '坐标';

COMMENT ON COLUMN flow_skip.create_time IS '创建时间';

COMMENT ON COLUMN flow_skip.create_by IS '创建人';

COMMENT ON COLUMN flow_skip.update_time IS '更新时间';

COMMENT ON COLUMN flow_skip.update_by IS '更新人';

COMMENT ON COLUMN flow_skip.del_flag IS '删除标志';

COMMENT ON COLUMN flow_skip.tenant_id IS '租户id';

COMMENT ON TABLE flow_skip IS '节点跳转关联表';

COMMENT ON COLUMN flow_task.id IS '主键id';

COMMENT ON COLUMN flow_task.definition_id IS '对应flow_definition表的id';

COMMENT ON COLUMN flow_task.instance_id IS '对应flow_instance表的id';

COMMENT ON COLUMN flow_task.node_code IS '节点编码';

COMMENT ON COLUMN flow_task.node_name IS '节点名称';

COMMENT ON COLUMN flow_task.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';

COMMENT ON COLUMN flow_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';

COMMENT ON COLUMN flow_task.form_custom IS '审批表单是否自定义（Y是 N否）';

COMMENT ON COLUMN flow_task.form_path IS '审批表单路径';

COMMENT ON COLUMN flow_task.create_time IS '创建时间';

COMMENT ON COLUMN flow_task.create_by IS '创建人';

COMMENT ON COLUMN flow_task.update_time IS '更新时间';

COMMENT ON COLUMN flow_task.update_by IS '更新人';

COMMENT ON COLUMN flow_task.del_flag IS '删除标志';

COMMENT ON COLUMN flow_task.tenant_id IS '租户id';

COMMENT ON TABLE flow_task IS '待办任务表';

COMMENT ON COLUMN flow_user.id IS '主键id';

COMMENT ON COLUMN flow_user.type IS '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）';

COMMENT ON COLUMN flow_user.processed_by IS '权限人';

COMMENT ON COLUMN flow_user.associated IS '任务表id';

COMMENT ON COLUMN flow_user.create_time IS '创建时间';

COMMENT ON COLUMN flow_user.create_by IS '创建人';

COMMENT ON COLUMN flow_user.update_time IS '更新时间';

COMMENT ON COLUMN flow_user.update_by IS '更新人';

COMMENT ON COLUMN flow_user.del_flag IS '删除标志';

COMMENT ON COLUMN flow_user.tenant_id IS '租户id';

COMMENT ON TABLE flow_user IS '流程用户表';

COMMENT ON COLUMN hrm_attendance_clock.id IS '打卡记录编号';

COMMENT ON COLUMN hrm_attendance_clock.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_attendance_clock.clock_time IS '打卡时间';

COMMENT ON COLUMN hrm_attendance_clock.type IS '打卡类型 枚举  字典';

COMMENT ON COLUMN hrm_attendance_clock.attendance_time IS '应打卡时间';

COMMENT ON COLUMN hrm_attendance_clock.source_type IS '打卡来源 枚举  字典';

COMMENT ON COLUMN hrm_attendance_clock.status IS '打卡状态 枚举  字典';

COMMENT ON COLUMN hrm_attendance_clock.stage IS '打卡阶段 枚举';

COMMENT ON COLUMN hrm_attendance_clock.address IS '打卡地址';

COMMENT ON COLUMN hrm_attendance_clock.longitude IS '经度';

COMMENT ON COLUMN hrm_attendance_clock.latitude IS '纬度';

COMMENT ON COLUMN hrm_attendance_clock.ssid IS 'WiFi 名称';

COMMENT ON COLUMN hrm_attendance_clock.mac IS 'WiFi MAC 地址';

COMMENT ON COLUMN hrm_attendance_clock.remark IS '备注';

COMMENT ON COLUMN hrm_attendance_clock.creator IS '创建者';

COMMENT ON COLUMN hrm_attendance_clock.create_time IS '创建时间';

COMMENT ON COLUMN hrm_attendance_clock.updater IS '更新者';

COMMENT ON COLUMN hrm_attendance_clock.update_time IS '更新时间';

COMMENT ON COLUMN hrm_attendance_clock.deleted IS '是否删除';

COMMENT ON TABLE hrm_attendance_clock IS 'HRM 考勤打卡记录';

COMMENT ON COLUMN hrm_attendance_group.id IS '考勤组编号';

COMMENT ON COLUMN hrm_attendance_group.name IS '考勤组名称';

COMMENT ON COLUMN hrm_attendance_group.dept_ids IS '适用部门编号';

COMMENT ON COLUMN hrm_attendance_group.employee_ids IS '适用员工编号';

COMMENT ON COLUMN hrm_attendance_group.open_wifi_card IS '是否启用 WiFi 打卡';

COMMENT ON COLUMN hrm_attendance_group.open_point_card IS '是否启用定位打卡';

COMMENT ON COLUMN hrm_attendance_group.shifts IS '班次配置';

COMMENT ON COLUMN hrm_attendance_group.rest IS '是否法定节假日休息';

COMMENT ON COLUMN hrm_attendance_group.special_dates IS '特殊日期设置数组';

COMMENT ON COLUMN hrm_attendance_group.points IS '打卡地点配置';

COMMENT ON COLUMN hrm_attendance_group.wifis IS 'WiFi 打卡配置';

COMMENT ON COLUMN hrm_attendance_group.deduct_rule IS '扣款规则';

COMMENT ON COLUMN hrm_attendance_group.default_status IS '是否默认考勤组';

COMMENT ON COLUMN hrm_attendance_group.creator IS '创建者';

COMMENT ON COLUMN hrm_attendance_group.create_time IS '创建时间';

COMMENT ON COLUMN hrm_attendance_group.updater IS '更新者';

COMMENT ON COLUMN hrm_attendance_group.update_time IS '更新时间';

COMMENT ON COLUMN hrm_attendance_group.deleted IS '是否删除';

COMMENT ON TABLE hrm_attendance_group IS 'HRM 考勤组';

COMMENT ON COLUMN hrm_attendance_holiday.id IS '节假日编号';

COMMENT ON COLUMN hrm_attendance_holiday.date IS '日期';

COMMENT ON COLUMN hrm_attendance_holiday.type IS '日期类型 枚举  字典';

COMMENT ON COLUMN hrm_attendance_holiday.creator IS '创建者';

COMMENT ON COLUMN hrm_attendance_holiday.create_time IS '创建时间';

COMMENT ON COLUMN hrm_attendance_holiday.updater IS '更新者';

COMMENT ON COLUMN hrm_attendance_holiday.update_time IS '更新时间';

COMMENT ON COLUMN hrm_attendance_holiday.deleted IS '是否删除';

COMMENT ON TABLE hrm_attendance_holiday IS 'HRM 考勤节假日';

COMMENT ON COLUMN hrm_attendance_leave.id IS '请假记录编号';

COMMENT ON COLUMN hrm_attendance_leave.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_attendance_leave.type IS '请假类型 字典';

COMMENT ON COLUMN hrm_attendance_leave.start_time IS '请假开始时间';

COMMENT ON COLUMN hrm_attendance_leave.end_time IS '请假结束时间';

COMMENT ON COLUMN hrm_attendance_leave.day IS '请假时长';

COMMENT ON COLUMN hrm_attendance_leave.reason IS '请假理由';

COMMENT ON COLUMN hrm_attendance_leave.remark IS '备注';

COMMENT ON COLUMN hrm_attendance_leave.approval_status IS '审批状态 枚举';

COMMENT ON COLUMN hrm_attendance_leave.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN hrm_attendance_leave.approval_time IS '审批完成时间';

COMMENT ON COLUMN hrm_attendance_leave.approval_reason IS '审批或取消原因';

COMMENT ON COLUMN hrm_attendance_leave.creator IS '创建者';

COMMENT ON COLUMN hrm_attendance_leave.create_time IS '创建时间';

COMMENT ON COLUMN hrm_attendance_leave.updater IS '更新者';

COMMENT ON COLUMN hrm_attendance_leave.update_time IS '更新时间';

COMMENT ON COLUMN hrm_attendance_leave.deleted IS '是否删除';

COMMENT ON TABLE hrm_attendance_leave IS 'HRM 考勤请假';

COMMENT ON COLUMN hrm_config.id IS '编号';

COMMENT ON COLUMN hrm_config.type IS '配置类型 枚举';

COMMENT ON COLUMN hrm_config.value IS '配置值';

COMMENT ON COLUMN hrm_config.sort IS '显示顺序';

COMMENT ON COLUMN hrm_config.creator IS '创建者';

COMMENT ON COLUMN hrm_config.create_time IS '创建时间';

COMMENT ON COLUMN hrm_config.updater IS '更新者';

COMMENT ON COLUMN hrm_config.update_time IS '更新时间';

COMMENT ON COLUMN hrm_config.deleted IS '是否删除';

COMMENT ON TABLE hrm_config IS 'HRM 通用业务配置';

COMMENT ON COLUMN hrm_employee.id IS '员工编号';

COMMENT ON COLUMN hrm_employee.name IS '员工姓名';

COMMENT ON COLUMN hrm_employee.job_number IS '工号';

COMMENT ON COLUMN hrm_employee.user_id IS '后台用户编号 允许为空，用于待入职、离职历史等没有登录账号的员工档案';

COMMENT ON COLUMN hrm_employee.mobile IS '手机号码';

COMMENT ON COLUMN hrm_employee.country IS '国家或地区';

COMMENT ON COLUMN hrm_employee.nation IS '民族';

COMMENT ON COLUMN hrm_employee.id_type IS '证件类型 枚举';

COMMENT ON COLUMN hrm_employee.id_number IS '证件号码';

COMMENT ON COLUMN hrm_employee.sex IS '性别 枚举';

COMMENT ON COLUMN hrm_employee.email IS '邮箱';

COMMENT ON COLUMN hrm_employee.native_place IS '籍贯';

COMMENT ON COLUMN hrm_employee.birthday IS '出生时间';

COMMENT ON COLUMN hrm_employee.age IS '年龄';

COMMENT ON COLUMN hrm_employee.address IS '户籍地址';

COMMENT ON COLUMN hrm_employee.highest_education IS '最高学历 枚举  字典';

COMMENT ON COLUMN hrm_employee.dept_id IS '部门编号';

COMMENT ON COLUMN hrm_employee.leader_employee_id IS '直属上级员工编号';

COMMENT ON COLUMN hrm_employee.entry_status IS '入职状态 枚举  字典';

COMMENT ON COLUMN hrm_employee.status IS '员工状态 枚举  字典';

COMMENT ON COLUMN hrm_employee.type IS '聘用形式 枚举  字典';

COMMENT ON COLUMN hrm_employee.entry_time IS '入职时间';

COMMENT ON COLUMN hrm_employee.probation IS '试用期，单位：月 0 表示无试用期';

COMMENT ON COLUMN hrm_employee.regular_time IS '转正时间';

COMMENT ON COLUMN hrm_employee.leave_time IS '离职时间';

COMMENT ON COLUMN hrm_employee.post_name IS '职位名称';

COMMENT ON COLUMN hrm_employee.post_level IS '岗位职级';

COMMENT ON COLUMN hrm_employee.work_city IS '工作城市';

COMMENT ON COLUMN hrm_employee.work_address IS '工作地址';

COMMENT ON COLUMN hrm_employee.work_detail_address IS '工作详细地址';

COMMENT ON COLUMN hrm_employee.channel_id IS '招聘渠道编号';

COMMENT ON COLUMN hrm_employee.company_age_start_time IS '司龄开始时间';

COMMENT ON COLUMN hrm_employee.company_age IS '司龄，单位：年';

COMMENT ON COLUMN hrm_employee.candidate_id IS '招聘候选人编号';

COMMENT ON COLUMN hrm_employee.celebration_opt_out IS '本人不参与生日/周年庆祝';

COMMENT ON COLUMN hrm_employee.remark IS '备注';

COMMENT ON COLUMN hrm_employee.creator IS '创建者';

COMMENT ON COLUMN hrm_employee.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee.updater IS '更新者';

COMMENT ON COLUMN hrm_employee.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee IS 'HRM 员工档案';

COMMENT ON COLUMN hrm_employee_certificate.id IS '证书编号';

COMMENT ON COLUMN hrm_employee_certificate.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_certificate.name IS '证书名称';

COMMENT ON COLUMN hrm_employee_certificate.level IS '证书级别';

COMMENT ON COLUMN hrm_employee_certificate.no IS '证书编号';

COMMENT ON COLUMN hrm_employee_certificate.start_time IS '有效开始日期';

COMMENT ON COLUMN hrm_employee_certificate.end_time IS '有效结束日期';

COMMENT ON COLUMN hrm_employee_certificate.issuing_authority IS '发证机构';

COMMENT ON COLUMN hrm_employee_certificate.issuing_time IS '发证日期';

COMMENT ON COLUMN hrm_employee_certificate.remark IS '备注';

COMMENT ON COLUMN hrm_employee_certificate.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_certificate.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_certificate.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_certificate.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_certificate.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_certificate.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_certificate IS 'HRM 员工证书';

COMMENT ON COLUMN hrm_employee_change_record.id IS '异动记录编号';

COMMENT ON COLUMN hrm_employee_change_record.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_change_record.type IS '异动类型 枚举';

COMMENT ON COLUMN hrm_employee_change_record.reason IS '异动原因 枚举';

COMMENT ON COLUMN hrm_employee_change_record.old_dept_id IS '原部门编号';

COMMENT ON COLUMN hrm_employee_change_record.new_dept_id IS '新部门编号';

COMMENT ON COLUMN hrm_employee_change_record.old_post_name IS '原岗位';

COMMENT ON COLUMN hrm_employee_change_record.new_post_name IS '新岗位';

COMMENT ON COLUMN hrm_employee_change_record.old_post_level IS '原职级';

COMMENT ON COLUMN hrm_employee_change_record.new_post_level IS '新职级';

COMMENT ON COLUMN hrm_employee_change_record.old_work_address IS '原工作地点';

COMMENT ON COLUMN hrm_employee_change_record.new_work_address IS '新工作地点';

COMMENT ON COLUMN hrm_employee_change_record.old_leader_employee_id IS '原直属上级员工编号';

COMMENT ON COLUMN hrm_employee_change_record.new_leader_employee_id IS '新直属上级员工编号';

COMMENT ON COLUMN hrm_employee_change_record.probation IS '试用期，单位：月';

COMMENT ON COLUMN hrm_employee_change_record.effect_time IS '生效日期';

COMMENT ON COLUMN hrm_employee_change_record.applied_time IS '实际生效时间';

COMMENT ON COLUMN hrm_employee_change_record.remark IS '备注';

COMMENT ON COLUMN hrm_employee_change_record.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_change_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_change_record.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_change_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_change_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_change_record IS 'HRM 员工异动记录';

COMMENT ON COLUMN hrm_employee_contact.id IS '联系人编号';

COMMENT ON COLUMN hrm_employee_contact.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_contact.name IS '联系人名称';

COMMENT ON COLUMN hrm_employee_contact.relation IS '与员工的关系';

COMMENT ON COLUMN hrm_employee_contact.phone IS '联系人电话';

COMMENT ON COLUMN hrm_employee_contact.work_unit IS '联系人工作单位';

COMMENT ON COLUMN hrm_employee_contact.post_name IS '联系人职务';

COMMENT ON COLUMN hrm_employee_contact.address IS '联系人地址';

COMMENT ON COLUMN hrm_employee_contact.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_contact.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_contact.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_contact.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_contact.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_contact.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_contact IS 'HRM 员工联系人';

COMMENT ON COLUMN hrm_employee_contract.id IS '合同编号';

COMMENT ON COLUMN hrm_employee_contract.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_contract.no IS '合同编号';

COMMENT ON COLUMN hrm_employee_contract.type IS '合同类型 枚举';

COMMENT ON COLUMN hrm_employee_contract.start_time IS '合同开始日期';

COMMENT ON COLUMN hrm_employee_contract.end_time IS '合同结束日期';

COMMENT ON COLUMN hrm_employee_contract.term IS '合同期限，单位：年';

COMMENT ON COLUMN hrm_employee_contract.status IS '合同状态 枚举';

COMMENT ON COLUMN hrm_employee_contract.sign_company IS '签约公司';

COMMENT ON COLUMN hrm_employee_contract.sign_time IS '合同签订日期';

COMMENT ON COLUMN hrm_employee_contract.remark IS '备注';

COMMENT ON COLUMN hrm_employee_contract.expire_remind IS '是否到期提醒';

COMMENT ON COLUMN hrm_employee_contract.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_contract.file_urls IS '附件地址数组';

COMMENT ON COLUMN hrm_employee_contract.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_contract.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_contract.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_contract.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_contract.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_contract IS 'HRM 员工合同';

COMMENT ON COLUMN hrm_employee_education_experience.id IS '教育经历编号';

COMMENT ON COLUMN hrm_employee_education_experience.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_education_experience.education IS '学历 枚举  字典';

COMMENT ON COLUMN hrm_employee_education_experience.graduate_school IS '毕业院校';

COMMENT ON COLUMN hrm_employee_education_experience.major IS '专业';

COMMENT ON COLUMN hrm_employee_education_experience.admission_time IS '入学日期';

COMMENT ON COLUMN hrm_employee_education_experience.graduation_time IS '毕业日期';

COMMENT ON COLUMN hrm_employee_education_experience.teaching_methods IS '教学方式 枚举';

COMMENT ON COLUMN hrm_employee_education_experience.first_degree IS '是否第一学历';

COMMENT ON COLUMN hrm_employee_education_experience.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_education_experience.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_education_experience.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_education_experience.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_education_experience.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_education_experience.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_education_experience IS 'HRM 员工教育经历';

COMMENT ON COLUMN hrm_employee_file.id IS '附件编号';

COMMENT ON COLUMN hrm_employee_file.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_file.type IS '附件类型 枚举';

COMMENT ON COLUMN hrm_employee_file.url IS '附件地址';

COMMENT ON COLUMN hrm_employee_file.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_file.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_file.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_file.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_file.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_file IS 'HRM 员工材料附件';

COMMENT ON COLUMN hrm_employee_personal_note.employee_id IS '所属员工编号';

COMMENT ON COLUMN hrm_employee_personal_note.content IS '备忘内容';

COMMENT ON COLUMN hrm_employee_personal_note.reminder_time IS '提醒时间';

COMMENT ON COLUMN hrm_employee_personal_note.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_personal_note.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_personal_note.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_personal_note.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_personal_note.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_personal_note IS 'HRM 员工个人备忘';

COMMENT ON COLUMN hrm_employee_quit_info.id IS '离职信息编号';

COMMENT ON COLUMN hrm_employee_quit_info.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_quit_info.plan_quit_time IS '计划离职时间';

COMMENT ON COLUMN hrm_employee_quit_info.apply_quit_time IS '申请离职时间';

COMMENT ON COLUMN hrm_employee_quit_info.salary_settlement_time IS '薪资结算时间';

COMMENT ON COLUMN hrm_employee_quit_info.type IS '离职类型 枚举';

COMMENT ON COLUMN hrm_employee_quit_info.reason IS '离职原因 枚举';

COMMENT ON COLUMN hrm_employee_quit_info.remark IS '备注';

COMMENT ON COLUMN hrm_employee_quit_info.old_employee_status IS '离职前员工状态 枚举';

COMMENT ON COLUMN hrm_employee_quit_info.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_quit_info.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_quit_info.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_quit_info.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_quit_info.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_quit_info IS 'HRM 员工离职信息';

COMMENT ON COLUMN hrm_employee_salary_card.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_salary_card.bank_card_number IS '银行卡号';

COMMENT ON COLUMN hrm_employee_salary_card.bank_area_id IS '开户地区编号';

COMMENT ON COLUMN hrm_employee_salary_card.bank_name IS '银行名称';

COMMENT ON COLUMN hrm_employee_salary_card.bank_branch_name IS '开户支行名称';

COMMENT ON COLUMN hrm_employee_salary_card.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_salary_card.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_salary_card.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_salary_card.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_salary_card.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_salary_card IS 'HRM 员工工资卡';

COMMENT ON COLUMN hrm_employee_training_experience.id IS '培训经历编号';

COMMENT ON COLUMN hrm_employee_training_experience.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_training_experience.course IS '培训课程';

COMMENT ON COLUMN hrm_employee_training_experience.organization_name IS '培训机构名称';

COMMENT ON COLUMN hrm_employee_training_experience.start_time IS '培训开始日期';

COMMENT ON COLUMN hrm_employee_training_experience.end_time IS '培训结束日期';

COMMENT ON COLUMN hrm_employee_training_experience.duration IS '培训时长';

COMMENT ON COLUMN hrm_employee_training_experience.result IS '培训成绩';

COMMENT ON COLUMN hrm_employee_training_experience.certificate_name IS '培训证书名称';

COMMENT ON COLUMN hrm_employee_training_experience.remark IS '备注';

COMMENT ON COLUMN hrm_employee_training_experience.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_training_experience.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_training_experience.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_training_experience.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_training_experience.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_training_experience.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_training_experience IS 'HRM 员工培训经历';

COMMENT ON TABLE hrm_employee_wecom_sync IS '企微花名册日期同步基线';

COMMENT ON COLUMN hrm_employee_work_experience.id IS '工作经历编号';

COMMENT ON COLUMN hrm_employee_work_experience.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_employee_work_experience.work_unit IS '工作单位';

COMMENT ON COLUMN hrm_employee_work_experience.post_name IS '职务';

COMMENT ON COLUMN hrm_employee_work_experience.start_time IS '工作开始日期';

COMMENT ON COLUMN hrm_employee_work_experience.end_time IS '工作结束日期';

COMMENT ON COLUMN hrm_employee_work_experience.reason IS '离职原因';

COMMENT ON COLUMN hrm_employee_work_experience.witness_name IS '证明人';

COMMENT ON COLUMN hrm_employee_work_experience.witness_phone IS '证明人手机号';

COMMENT ON COLUMN hrm_employee_work_experience.remark IS '工作备注';

COMMENT ON COLUMN hrm_employee_work_experience.sort IS '显示顺序';

COMMENT ON COLUMN hrm_employee_work_experience.creator IS '创建者';

COMMENT ON COLUMN hrm_employee_work_experience.create_time IS '创建时间';

COMMENT ON COLUMN hrm_employee_work_experience.updater IS '更新者';

COMMENT ON COLUMN hrm_employee_work_experience.update_time IS '更新时间';

COMMENT ON COLUMN hrm_employee_work_experience.deleted IS '是否删除';

COMMENT ON TABLE hrm_employee_work_experience IS 'HRM 员工工作经历';

COMMENT ON COLUMN hrm_insurance_employee_info.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_insurance_employee_info.first_social_security IS '是否首次参保';

COMMENT ON COLUMN hrm_insurance_employee_info.first_accumulation_fund IS '是否首次缴纳公积金';

COMMENT ON COLUMN hrm_insurance_employee_info.social_security_number IS '社保账号';

COMMENT ON COLUMN hrm_insurance_employee_info.accumulation_fund_number IS '公积金账号';

COMMENT ON COLUMN hrm_insurance_employee_info.social_security_start_month IS '社保起缴月份';

COMMENT ON COLUMN hrm_insurance_employee_info.scheme_id IS '社保方案编号';

COMMENT ON COLUMN hrm_insurance_employee_info.creator IS '创建者';

COMMENT ON COLUMN hrm_insurance_employee_info.create_time IS '创建时间';

COMMENT ON COLUMN hrm_insurance_employee_info.updater IS '更新者';

COMMENT ON COLUMN hrm_insurance_employee_info.update_time IS '更新时间';

COMMENT ON COLUMN hrm_insurance_employee_info.deleted IS '是否删除';

COMMENT ON TABLE hrm_insurance_employee_info IS 'HRM 员工参保信息';

COMMENT ON COLUMN hrm_insurance_month_employee_record.id IS '编号';

COMMENT ON COLUMN hrm_insurance_month_employee_record.month_record_id IS '月度社保表编号';

COMMENT ON COLUMN hrm_insurance_month_employee_record.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_insurance_month_employee_record.scheme_id IS '社保方案编号';

COMMENT ON COLUMN hrm_insurance_month_employee_record.year IS '年份';

COMMENT ON COLUMN hrm_insurance_month_employee_record.month IS '月份';

COMMENT ON COLUMN hrm_insurance_month_employee_record.personal_insurance_amount IS '个人社保金额';

COMMENT ON COLUMN hrm_insurance_month_employee_record.personal_provident_fund_amount IS '个人公积金金额';

COMMENT ON COLUMN hrm_insurance_month_employee_record.corporate_insurance_amount IS '公司社保金额';

COMMENT ON COLUMN hrm_insurance_month_employee_record.corporate_provident_fund_amount IS '公司公积金金额';

COMMENT ON COLUMN hrm_insurance_month_employee_record.status IS '参保状态 枚举';

COMMENT ON COLUMN hrm_insurance_month_employee_record.projects IS '参保项目快照';

COMMENT ON COLUMN hrm_insurance_month_employee_record.creator IS '创建者';

COMMENT ON COLUMN hrm_insurance_month_employee_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_insurance_month_employee_record.updater IS '更新者';

COMMENT ON COLUMN hrm_insurance_month_employee_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_insurance_month_employee_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_insurance_month_employee_record IS 'HRM 员工月度社保记录';

COMMENT ON COLUMN hrm_insurance_month_record.id IS '编号';

COMMENT ON COLUMN hrm_insurance_month_record.title IS '标题';

COMMENT ON COLUMN hrm_insurance_month_record.year IS '年份';

COMMENT ON COLUMN hrm_insurance_month_record.month IS '月份';

COMMENT ON COLUMN hrm_insurance_month_record.insured_employee_count IS '参保人数';

COMMENT ON COLUMN hrm_insurance_month_record.stopped_employee_count IS '停保人数';

COMMENT ON COLUMN hrm_insurance_month_record.status IS '状态 枚举';

COMMENT ON COLUMN hrm_insurance_month_record.personal_insurance_amount IS '个人社保金额';

COMMENT ON COLUMN hrm_insurance_month_record.personal_provident_fund_amount IS '个人公积金金额';

COMMENT ON COLUMN hrm_insurance_month_record.corporate_insurance_amount IS '公司社保金额';

COMMENT ON COLUMN hrm_insurance_month_record.corporate_provident_fund_amount IS '公司公积金金额';

COMMENT ON COLUMN hrm_insurance_month_record.creator IS '创建者';

COMMENT ON COLUMN hrm_insurance_month_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_insurance_month_record.updater IS '更新者';

COMMENT ON COLUMN hrm_insurance_month_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_insurance_month_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_insurance_month_record IS 'HRM 月度社保表';

COMMENT ON COLUMN hrm_insurance_scheme.id IS '编号';

COMMENT ON COLUMN hrm_insurance_scheme.name IS '方案名称';

COMMENT ON COLUMN hrm_insurance_scheme.area_id IS '参保地区编号';

COMMENT ON COLUMN hrm_insurance_scheme.household_type IS '户籍类型';

COMMENT ON COLUMN hrm_insurance_scheme.type IS '方案类型 枚举';

COMMENT ON COLUMN hrm_insurance_scheme.creator IS '创建者';

COMMENT ON COLUMN hrm_insurance_scheme.create_time IS '创建时间';

COMMENT ON COLUMN hrm_insurance_scheme.updater IS '更新者';

COMMENT ON COLUMN hrm_insurance_scheme.update_time IS '更新时间';

COMMENT ON COLUMN hrm_insurance_scheme.deleted IS '是否删除';

COMMENT ON TABLE hrm_insurance_scheme IS 'HRM 社保方案';

COMMENT ON COLUMN hrm_insurance_scheme_project.id IS '编号';

COMMENT ON COLUMN hrm_insurance_scheme_project.scheme_id IS '社保方案编号';

COMMENT ON COLUMN hrm_insurance_scheme_project.type IS '项目类型 枚举';

COMMENT ON COLUMN hrm_insurance_scheme_project.name IS '项目名称';

COMMENT ON COLUMN hrm_insurance_scheme_project.base_amount IS '缴纳基数';

COMMENT ON COLUMN hrm_insurance_scheme_project.corporate_rate IS '公司缴纳比例';

COMMENT ON COLUMN hrm_insurance_scheme_project.personal_rate IS '个人缴纳比例';

COMMENT ON COLUMN hrm_insurance_scheme_project.corporate_amount IS '公司缴纳金额';

COMMENT ON COLUMN hrm_insurance_scheme_project.personal_amount IS '个人缴纳金额';

COMMENT ON COLUMN hrm_insurance_scheme_project.creator IS '创建者';

COMMENT ON COLUMN hrm_insurance_scheme_project.create_time IS '创建时间';

COMMENT ON COLUMN hrm_insurance_scheme_project.updater IS '更新者';

COMMENT ON COLUMN hrm_insurance_scheme_project.update_time IS '更新时间';

COMMENT ON COLUMN hrm_insurance_scheme_project.deleted IS '是否删除';

COMMENT ON TABLE hrm_insurance_scheme_project IS 'HRM 社保方案项目';

COMMENT ON COLUMN hrm_performance_assessment.id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment.plan_id IS '绩效计划编号';

COMMENT ON COLUMN hrm_performance_assessment.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_performance_assessment.status IS '考核状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment.process_status IS '流程处理状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment.stage_type IS '当前业务阶段类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment.stage_sort IS '当前阶段排序';

COMMENT ON COLUMN hrm_performance_assessment.score IS '综合得分';

COMMENT ON COLUMN hrm_performance_assessment.result_level IS '结果等级';

COMMENT ON COLUMN hrm_performance_assessment.coefficient IS '绩效系数';

COMMENT ON COLUMN hrm_performance_assessment.target_confirmation_result IS '目标确认结果 枚举';

COMMENT ON COLUMN hrm_performance_assessment.target_confirmation_comment IS '目标确认意见';

COMMENT ON COLUMN hrm_performance_assessment.target_confirmation_time IS '目标确认时间';

COMMENT ON COLUMN hrm_performance_assessment.self_comment IS '自评说明';

COMMENT ON COLUMN hrm_performance_assessment.reviewer_comment IS '他评说明';

COMMENT ON COLUMN hrm_performance_assessment.result_comment IS '结果说明';

COMMENT ON COLUMN hrm_performance_assessment.result_confirmation_time IS '结果确认时间';

COMMENT ON COLUMN hrm_performance_assessment.result_audit_status IS '结果审核状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment.result_audit_time IS '结果审核时间';

COMMENT ON COLUMN hrm_performance_assessment.result_audit_reason IS '结果审核意见';

COMMENT ON COLUMN hrm_performance_assessment.appeal_reason IS '申诉原因';

COMMENT ON COLUMN hrm_performance_assessment.appeal_file_urls IS '申诉附件地址列表';

COMMENT ON COLUMN hrm_performance_assessment.appeal_submit_time IS '申诉提交时间';

COMMENT ON COLUMN hrm_performance_assessment.appeal_status IS '申诉状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment.appeal_time IS '申诉完成时间';

COMMENT ON COLUMN hrm_performance_assessment.appeal_comment IS '申诉处理意见';

COMMENT ON COLUMN hrm_performance_assessment.archive_time IS '归档时间';

COMMENT ON COLUMN hrm_performance_assessment.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment IS 'HRM 员工绩效考核';

COMMENT ON COLUMN hrm_performance_assessment_action_record.id IS '动作记录编号';

COMMENT ON COLUMN hrm_performance_assessment_action_record.assessment_id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment_action_record.stage_id IS '员工绩效考核阶段编号';

COMMENT ON COLUMN hrm_performance_assessment_action_record.employee_id IS '操作员工编号';

COMMENT ON COLUMN hrm_performance_assessment_action_record.type IS '动作类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_action_record.title IS '动作标题';

COMMENT ON COLUMN hrm_performance_assessment_action_record.content IS '动作内容';

COMMENT ON COLUMN hrm_performance_assessment_action_record.file_urls IS '附件地址列表';

COMMENT ON COLUMN hrm_performance_assessment_action_record.status IS '动作完成后的阶段状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment_action_record.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_action_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_action_record.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_action_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_action_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_action_record IS 'HRM 员工绩效考核动作记录';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.id IS '申诉记录编号';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.assessment_id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.stage_id IS '员工绩效考核阶段编号';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.status IS '处理状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_appeal_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_appeal_record IS 'HRM 员工绩效考核申诉记录';

COMMENT ON COLUMN hrm_performance_assessment_dimension.id IS '考核维度编号';

COMMENT ON COLUMN hrm_performance_assessment_dimension.assessment_id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment_dimension.name IS '维度名称';

COMMENT ON COLUMN hrm_performance_assessment_dimension.quota_type IS '指标类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_dimension.weight IS '维度权重';

COMMENT ON COLUMN hrm_performance_assessment_dimension.remark IS '备注';

COMMENT ON COLUMN hrm_performance_assessment_dimension.allow_edit IS '是否允许员工编辑指标';

COMMENT ON COLUMN hrm_performance_assessment_dimension.sort IS '排序';

COMMENT ON COLUMN hrm_performance_assessment_dimension.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_dimension.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_dimension.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_dimension.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_dimension.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_dimension IS 'HRM 员工绩效考核维度';

COMMENT ON COLUMN hrm_performance_assessment_quota.id IS '考核指标编号';

COMMENT ON COLUMN hrm_performance_assessment_quota.assessment_id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment_quota.dimension_id IS '考核维度编号';

COMMENT ON COLUMN hrm_performance_assessment_quota.preset IS '是否预置指标';

COMMENT ON COLUMN hrm_performance_assessment_quota.name IS '指标名称';

COMMENT ON COLUMN hrm_performance_assessment_quota.description IS '指标说明';

COMMENT ON COLUMN hrm_performance_assessment_quota.standard IS '评分标准';

COMMENT ON COLUMN hrm_performance_assessment_quota.weight IS '指标权重';

COMMENT ON COLUMN hrm_performance_assessment_quota.score_type IS '评分类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_quota.target_value IS '目标值';

COMMENT ON COLUMN hrm_performance_assessment_quota.actual_value IS '实际值';

COMMENT ON COLUMN hrm_performance_assessment_quota.self_score IS '自评汇总分';

COMMENT ON COLUMN hrm_performance_assessment_quota.reviewer_score IS '他评汇总分';

COMMENT ON COLUMN hrm_performance_assessment_quota.final_score IS '最终指标得分';

COMMENT ON COLUMN hrm_performance_assessment_quota.sort IS '排序';

COMMENT ON COLUMN hrm_performance_assessment_quota.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_quota.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_quota.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_quota.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_quota.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_quota IS 'HRM 员工绩效考核指标';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.id IS '考核指标评分编号';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.assessment_stage_id IS '员工绩效考核阶段编号';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.assessment_quota_id IS '员工绩效考核指标编号';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.score IS '指标得分';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.comment IS '指标评语';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_quota_score.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_quota_score IS 'HRM 员工绩效考核指标评分';

COMMENT ON COLUMN hrm_performance_assessment_stage.id IS '考核阶段编号';

COMMENT ON COLUMN hrm_performance_assessment_stage.assessment_id IS '员工绩效考核编号';

COMMENT ON COLUMN hrm_performance_assessment_stage.type IS '业务阶段类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_stage.name IS '阶段名称';

COMMENT ON COLUMN hrm_performance_assessment_stage.handler_employee_id IS '实际处理员工编号';

COMMENT ON COLUMN hrm_performance_assessment_stage.rater_type IS '评分人类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_stage.weight IS '评分权重快照';

COMMENT ON COLUMN hrm_performance_assessment_stage.scoring_type IS '评分方式快照 枚举';

COMMENT ON COLUMN hrm_performance_assessment_stage.visible_content IS '评分内容可见范围快照 枚举';

COMMENT ON COLUMN hrm_performance_assessment_stage.required_setting IS '评语是否必填';

COMMENT ON COLUMN hrm_performance_assessment_stage.reject_authority IS '是否允许驳回';

COMMENT ON COLUMN hrm_performance_assessment_stage.sort IS '排序';

COMMENT ON COLUMN hrm_performance_assessment_stage.status IS '阶段处理状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment_stage.score IS '阶段汇总得分';

COMMENT ON COLUMN hrm_performance_assessment_stage.result_level IS '阶段结果等级';

COMMENT ON COLUMN hrm_performance_assessment_stage.comment IS '阶段意见';

COMMENT ON COLUMN hrm_performance_assessment_stage.reject_reason IS '最近一次驳回原因';

COMMENT ON COLUMN hrm_performance_assessment_stage.submit_time IS '提交时间';

COMMENT ON COLUMN hrm_performance_assessment_stage.deadline_time IS '截止时间';

COMMENT ON COLUMN hrm_performance_assessment_stage.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_stage.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_stage.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_stage.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_stage.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_stage IS 'HRM 员工绩效考核阶段';

COMMENT ON COLUMN hrm_performance_assessment_template.id IS '绩效考核模板编号';

COMMENT ON COLUMN hrm_performance_assessment_template.name IS '模板名称';

COMMENT ON COLUMN hrm_performance_assessment_template.illustrate IS '模板说明';

COMMENT ON COLUMN hrm_performance_assessment_template.score_calculation IS '计分方式 枚举';

COMMENT ON COLUMN hrm_performance_assessment_template.upper_limit_type IS '分数上限类型 枚举';

COMMENT ON COLUMN hrm_performance_assessment_template.upper_limit_score IS '分数上限';

COMMENT ON COLUMN hrm_performance_assessment_template.dimension_count IS '维度数量';

COMMENT ON COLUMN hrm_performance_assessment_template.quota_count IS '指标数量';

COMMENT ON COLUMN hrm_performance_assessment_template.dimensions IS '考核维度配置';

COMMENT ON COLUMN hrm_performance_assessment_template.status IS '状态 枚举';

COMMENT ON COLUMN hrm_performance_assessment_template.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_assessment_template.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_assessment_template.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_assessment_template.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_assessment_template.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_assessment_template IS 'HRM 绩效考核模板';

COMMENT ON COLUMN hrm_performance_plan.id IS '计划编号';

COMMENT ON COLUMN hrm_performance_plan.status IS '计划状态 枚举';

COMMENT ON COLUMN hrm_performance_plan.stage_type IS '当前业务阶段类型 枚举';

COMMENT ON COLUMN hrm_performance_plan.operation_type IS '当前可执行操作类型 枚举';

COMMENT ON COLUMN hrm_performance_plan.terminate_time IS '终止时间';

COMMENT ON COLUMN hrm_performance_plan.name IS '计划名称';

COMMENT ON COLUMN hrm_performance_plan.cycle_type IS '考核周期类型 枚举';

COMMENT ON COLUMN hrm_performance_plan.cycle IS '考核周期';

COMMENT ON COLUMN hrm_performance_plan.quarter IS '季度 枚举';

COMMENT ON COLUMN hrm_performance_plan.start_time IS '开始时间';

COMMENT ON COLUMN hrm_performance_plan.end_time IS '结束时间';

COMMENT ON COLUMN hrm_performance_plan.description IS '考核说明';

COMMENT ON COLUMN hrm_performance_plan.scopes IS '考评范围列表';

COMMENT ON COLUMN hrm_performance_plan.assessment_template_id IS '考核模板编号';

COMMENT ON COLUMN hrm_performance_plan.assessment_config IS '考核配置快照';

COMMENT ON COLUMN hrm_performance_plan.quota_setting_type IS '指标制定类型 枚举';

COMMENT ON COLUMN hrm_performance_plan.target_confirmation IS '是否开启目标确认';

COMMENT ON COLUMN hrm_performance_plan.target_confirmation_stage IS '目标确认节点';

COMMENT ON COLUMN hrm_performance_plan.review_stages IS '评分阶段列表';

COMMENT ON COLUMN hrm_performance_plan.result_audit IS '是否开启结果审核';

COMMENT ON COLUMN hrm_performance_plan.result_audit_stages IS '结果审核节点列表';

COMMENT ON COLUMN hrm_performance_plan.result_confirmation IS '是否开启结果确认';

COMMENT ON COLUMN hrm_performance_plan.appeal_stages IS '申诉处理节点列表';

COMMENT ON COLUMN hrm_performance_plan.appeal_timeout_days IS '申诉超期天数';

COMMENT ON COLUMN hrm_performance_plan.appeal_timeout_action IS '申诉超期处理动作 枚举';

COMMENT ON COLUMN hrm_performance_plan.result_template_id IS '结果模板编号';

COMMENT ON COLUMN hrm_performance_plan.result_config IS '结果配置快照';

COMMENT ON COLUMN hrm_performance_plan.sync_to_salary IS '是否同步到薪资';

COMMENT ON COLUMN hrm_performance_plan.paid_for_month IS '计薪月份';

COMMENT ON COLUMN hrm_performance_plan.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_plan.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_plan.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_plan.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_plan.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_plan IS 'HRM 绩效考核计划';

COMMENT ON COLUMN hrm_performance_result_template.id IS '绩效结果模板编号';

COMMENT ON COLUMN hrm_performance_result_template.name IS '模板名称';

COMMENT ON COLUMN hrm_performance_result_template.status IS '状态 枚举';

COMMENT ON COLUMN hrm_performance_result_template.levels IS '结果等级配置';

COMMENT ON COLUMN hrm_performance_result_template.creator IS '创建者';

COMMENT ON COLUMN hrm_performance_result_template.create_time IS '创建时间';

COMMENT ON COLUMN hrm_performance_result_template.updater IS '更新者';

COMMENT ON COLUMN hrm_performance_result_template.update_time IS '更新时间';

COMMENT ON COLUMN hrm_performance_result_template.deleted IS '是否删除';

COMMENT ON TABLE hrm_performance_result_template IS 'HRM 绩效结果模板';

COMMENT ON COLUMN hrm_recruit_candidate.id IS '候选人编号';

COMMENT ON COLUMN hrm_recruit_candidate.name IS '候选人姓名';

COMMENT ON COLUMN hrm_recruit_candidate.mobile IS '手机号码';

COMMENT ON COLUMN hrm_recruit_candidate.sex IS '性别 枚举';

COMMENT ON COLUMN hrm_recruit_candidate.age IS '年龄';

COMMENT ON COLUMN hrm_recruit_candidate.email IS '邮箱';

COMMENT ON COLUMN hrm_recruit_candidate.post_id IS '应聘职位编号';

COMMENT ON COLUMN hrm_recruit_candidate.stage_number IS '面试轮次';

COMMENT ON COLUMN hrm_recruit_candidate.work_time IS '工作年限，单位：年';

COMMENT ON COLUMN hrm_recruit_candidate.education IS '学历 字典';

COMMENT ON COLUMN hrm_recruit_candidate.graduate_school IS '毕业院校';

COMMENT ON COLUMN hrm_recruit_candidate.latest_work_place IS '最近工作单位';

COMMENT ON COLUMN hrm_recruit_candidate.channel_id IS '招聘渠道编号';

COMMENT ON COLUMN hrm_recruit_candidate.remark IS '备注';

COMMENT ON COLUMN hrm_recruit_candidate.status IS '候选人状态 枚举  字典';

COMMENT ON COLUMN hrm_recruit_candidate.eliminate IS '淘汰原因';

COMMENT ON COLUMN hrm_recruit_candidate.status_update_time IS '状态更新时间';

COMMENT ON COLUMN hrm_recruit_candidate.entry_time IS '入职时间';

COMMENT ON COLUMN hrm_recruit_candidate.resume_urls IS '简历附件地址数组';

COMMENT ON COLUMN hrm_recruit_candidate.creator IS '创建者';

COMMENT ON COLUMN hrm_recruit_candidate.create_time IS '创建时间';

COMMENT ON COLUMN hrm_recruit_candidate.updater IS '更新者';

COMMENT ON COLUMN hrm_recruit_candidate.update_time IS '更新时间';

COMMENT ON COLUMN hrm_recruit_candidate.deleted IS '是否删除';

COMMENT ON TABLE hrm_recruit_candidate IS 'HRM 招聘候选人';

COMMENT ON COLUMN hrm_recruit_channel.id IS '招聘渠道编号';

COMMENT ON COLUMN hrm_recruit_channel.system_flag IS '是否系统内置';

COMMENT ON COLUMN hrm_recruit_channel.status IS '状态 枚举';

COMMENT ON COLUMN hrm_recruit_channel.name IS '渠道名称';

COMMENT ON COLUMN hrm_recruit_channel.sort IS '显示顺序';

COMMENT ON COLUMN hrm_recruit_channel.remark IS '备注';

COMMENT ON COLUMN hrm_recruit_channel.creator IS '创建者';

COMMENT ON COLUMN hrm_recruit_channel.create_time IS '创建时间';

COMMENT ON COLUMN hrm_recruit_channel.updater IS '更新者';

COMMENT ON COLUMN hrm_recruit_channel.update_time IS '更新时间';

COMMENT ON COLUMN hrm_recruit_channel.deleted IS '是否删除';

COMMENT ON TABLE hrm_recruit_channel IS 'HRM 招聘渠道';

COMMENT ON COLUMN hrm_recruit_interview.id IS '面试编号';

COMMENT ON COLUMN hrm_recruit_interview.candidate_id IS '候选人编号';

COMMENT ON COLUMN hrm_recruit_interview.type IS '面试方式 字典';

COMMENT ON COLUMN hrm_recruit_interview.stage_number IS '面试轮次';

COMMENT ON COLUMN hrm_recruit_interview.interview_employee_id IS '主面试官员工编号';

COMMENT ON COLUMN hrm_recruit_interview.other_interview_employee_ids IS '其他面试官员工编号数组';

COMMENT ON COLUMN hrm_recruit_interview.interview_time IS '面试时间';

COMMENT ON COLUMN hrm_recruit_interview.address IS '面试地址';

COMMENT ON COLUMN hrm_recruit_interview.remark IS '备注';

COMMENT ON COLUMN hrm_recruit_interview.result IS '面试结果 枚举  字典';

COMMENT ON COLUMN hrm_recruit_interview.evaluate IS '面试评价';

COMMENT ON COLUMN hrm_recruit_interview.cancel_reason IS '取消原因';

COMMENT ON COLUMN hrm_recruit_interview.creator IS '创建者';

COMMENT ON COLUMN hrm_recruit_interview.create_time IS '创建时间';

COMMENT ON COLUMN hrm_recruit_interview.updater IS '更新者';

COMMENT ON COLUMN hrm_recruit_interview.update_time IS '更新时间';

COMMENT ON COLUMN hrm_recruit_interview.deleted IS '是否删除';

COMMENT ON TABLE hrm_recruit_interview IS 'HRM 招聘面试记录';

COMMENT ON COLUMN hrm_recruit_post.id IS '招聘职位编号';

COMMENT ON COLUMN hrm_recruit_post.post_name IS '职位名称';

COMMENT ON COLUMN hrm_recruit_post.dept_id IS '部门编号';

COMMENT ON COLUMN hrm_recruit_post.job_nature IS '工作性质 字典';

COMMENT ON COLUMN hrm_recruit_post.area_id IS '工作城市地区编号';

COMMENT ON COLUMN hrm_recruit_post.recruit_num IS '招聘人数';

COMMENT ON COLUMN hrm_recruit_post.reason IS '招聘原因';

COMMENT ON COLUMN hrm_recruit_post.work_time IS '工作经验要求 字典';

COMMENT ON COLUMN hrm_recruit_post.education_require IS '学历要求 字典';

COMMENT ON COLUMN hrm_recruit_post.min_salary IS '最低薪资 -1 表示面议';

COMMENT ON COLUMN hrm_recruit_post.max_salary IS '最高薪资 -1 表示面议';

COMMENT ON COLUMN hrm_recruit_post.salary_unit IS '薪资单位 字典';

COMMENT ON COLUMN hrm_recruit_post.min_age IS '最小年龄 -1 表示不限';

COMMENT ON COLUMN hrm_recruit_post.max_age IS '最大年龄 -1 表示不限';

COMMENT ON COLUMN hrm_recruit_post.latest_entry_time IS '最迟到岗时间';

COMMENT ON COLUMN hrm_recruit_post.owner_employee_id IS '招聘负责人员工编号';

COMMENT ON COLUMN hrm_recruit_post.interview_employee_ids IS '面试官员工编号数组';

COMMENT ON COLUMN hrm_recruit_post.description IS '职位描述';

COMMENT ON COLUMN hrm_recruit_post.emergency_level IS '紧急程度 字典';

COMMENT ON COLUMN hrm_recruit_post.post_type_id IS '职位类型编号';

COMMENT ON COLUMN hrm_recruit_post.status IS '职位状态 枚举  字典';

COMMENT ON COLUMN hrm_recruit_post.stop_reason IS '停止原因';

COMMENT ON COLUMN hrm_recruit_post.creator IS '创建者';

COMMENT ON COLUMN hrm_recruit_post.create_time IS '创建时间';

COMMENT ON COLUMN hrm_recruit_post.updater IS '更新者';

COMMENT ON COLUMN hrm_recruit_post.update_time IS '更新时间';

COMMENT ON COLUMN hrm_recruit_post.deleted IS '是否删除';

COMMENT ON TABLE hrm_recruit_post IS 'HRM 招聘职位';

COMMENT ON COLUMN hrm_recruit_post_type.id IS '职位类型编号';

COMMENT ON COLUMN hrm_recruit_post_type.name IS '类型名称';

COMMENT ON COLUMN hrm_recruit_post_type.parent_id IS '父类型编号';

COMMENT ON COLUMN hrm_recruit_post_type.sort IS '显示顺序';

COMMENT ON COLUMN hrm_recruit_post_type.status IS '状态 枚举';

COMMENT ON COLUMN hrm_recruit_post_type.creator IS '创建者';

COMMENT ON COLUMN hrm_recruit_post_type.create_time IS '创建时间';

COMMENT ON COLUMN hrm_recruit_post_type.updater IS '更新者';

COMMENT ON COLUMN hrm_recruit_post_type.update_time IS '更新时间';

COMMENT ON COLUMN hrm_recruit_post_type.deleted IS '是否删除';

COMMENT ON TABLE hrm_recruit_post_type IS 'HRM 招聘职位类型';

COMMENT ON COLUMN hrm_salary_change_record.id IS '编号';

COMMENT ON COLUMN hrm_salary_change_record.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_salary_change_record.type IS '记录类型 枚举';

COMMENT ON COLUMN hrm_salary_change_record.reason IS '调整原因 枚举';

COMMENT ON COLUMN hrm_salary_change_record.effect_time IS '生效时间';

COMMENT ON COLUMN hrm_salary_change_record.before_total IS '调整前正式薪资';

COMMENT ON COLUMN hrm_salary_change_record.after_total IS '调整后正式薪资';

COMMENT ON COLUMN hrm_salary_change_record.probation_before_total IS '调整前试用期薪资';

COMMENT ON COLUMN hrm_salary_change_record.probation_after_total IS '调整后试用期薪资';

COMMENT ON COLUMN hrm_salary_change_record.salary_options IS '正式薪资项后态快照';

COMMENT ON COLUMN hrm_salary_change_record.probation_salary_options IS '试用期薪资项后态快照';

COMMENT ON COLUMN hrm_salary_change_record.status IS '状态 枚举';

COMMENT ON COLUMN hrm_salary_change_record.remark IS '备注';

COMMENT ON COLUMN hrm_salary_change_record.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_change_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_change_record.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_change_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_change_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_change_record IS 'HRM 定薪/调薪记录';

COMMENT ON COLUMN hrm_salary_change_template.id IS '调薪模板编号';

COMMENT ON COLUMN hrm_salary_change_template.name IS '模板名称';

COMMENT ON COLUMN hrm_salary_change_template.default_status IS '是否默认模板';

COMMENT ON COLUMN hrm_salary_change_template.options IS '调薪项配置';

COMMENT ON COLUMN hrm_salary_change_template.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_change_template.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_change_template.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_change_template.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_change_template.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_change_template IS 'HRM 调薪模板';

COMMENT ON COLUMN hrm_salary_config.cycle_start_day IS '计薪周期开始日';

COMMENT ON COLUMN hrm_salary_config.cycle_end_day IS '计薪周期结束日';

COMMENT ON COLUMN hrm_salary_config.social_security_month_type IS '社保对应月份类型 枚举';

COMMENT ON COLUMN hrm_salary_config.start_year IS '工资开始年份';

COMMENT ON COLUMN hrm_salary_config.start_month IS '工资开始月份';

COMMENT ON COLUMN hrm_salary_config.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_config.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_config.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_config.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_config.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_config IS 'HRM 计薪配置';

COMMENT ON COLUMN hrm_salary_employee_info.id IS '编号';

COMMENT ON COLUMN hrm_salary_employee_info.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_salary_employee_info.change_reason IS '定薪、调薪原因 枚举';

COMMENT ON COLUMN hrm_salary_employee_info.effect_time IS '生效时间';

COMMENT ON COLUMN hrm_salary_employee_info.change_type IS '薪资信息变更类型 枚举';

COMMENT ON COLUMN hrm_salary_employee_info.probation_salary IS '试用期工资';

COMMENT ON COLUMN hrm_salary_employee_info.regular_salary IS '正式工资';

COMMENT ON COLUMN hrm_salary_employee_info.remark IS '备注';

COMMENT ON COLUMN hrm_salary_employee_info.salary_options IS '正式薪资项 薪资项随员工薪资信息整体维护';

COMMENT ON COLUMN hrm_salary_employee_info.probation_salary_options IS '试用期薪资项 薪资项随员工薪资信息整体维护';

COMMENT ON COLUMN hrm_salary_employee_info.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_employee_info.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_employee_info.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_employee_info.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_employee_info.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_employee_info IS 'HRM 员工薪资信息';

COMMENT ON COLUMN hrm_salary_group.name IS '薪资组名称';

COMMENT ON COLUMN hrm_salary_group.salary_standard IS '月计薪标准';

COMMENT ON COLUMN hrm_salary_group.change_rule IS '转正、调薪月规则';

COMMENT ON COLUMN hrm_salary_group.dept_ids IS '适用部门编号列表';

COMMENT ON COLUMN hrm_salary_group.employee_ids IS '适用员工编号列表';

COMMENT ON COLUMN hrm_salary_group.tax_rule_id IS '计税规则编号';

COMMENT ON COLUMN hrm_salary_group.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_group.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_group.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_group.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_group.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_group IS 'HRM 薪资组';

COMMENT ON COLUMN hrm_salary_month_employee_record.id IS '编号';

COMMENT ON COLUMN hrm_salary_month_employee_record.month_record_id IS '月度工资表编号';

COMMENT ON COLUMN hrm_salary_month_employee_record.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_salary_month_employee_record.actual_work_day IS '实际工作天数';

COMMENT ON COLUMN hrm_salary_month_employee_record.need_work_day IS '应工作天数';

COMMENT ON COLUMN hrm_salary_month_employee_record.year IS '年份';

COMMENT ON COLUMN hrm_salary_month_employee_record.month IS '月份';

COMMENT ON COLUMN hrm_salary_month_employee_record.expected_pay_salary IS '应发工资';

COMMENT ON COLUMN hrm_salary_month_employee_record.taxable_salary IS '应税工资';

COMMENT ON COLUMN hrm_salary_month_employee_record.personal_tax IS '个人所得税';

COMMENT ON COLUMN hrm_salary_month_employee_record.real_pay_salary IS '实发工资';

COMMENT ON COLUMN hrm_salary_month_employee_record.option_values IS '薪资项值快照 薪资项值随员工月度工资记录整体维护';

COMMENT ON COLUMN hrm_salary_month_employee_record.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_month_employee_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_month_employee_record.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_month_employee_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_month_employee_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_month_employee_record IS 'HRM 员工月度工资记录';

COMMENT ON COLUMN hrm_salary_month_record.id IS '编号';

COMMENT ON COLUMN hrm_salary_month_record.title IS '工资表标题';

COMMENT ON COLUMN hrm_salary_month_record.year IS '年份';

COMMENT ON COLUMN hrm_salary_month_record.month IS '月份';

COMMENT ON COLUMN hrm_salary_month_record.employee_count IS '员工人数';

COMMENT ON COLUMN hrm_salary_month_record.start_time IS '计薪开始日期';

COMMENT ON COLUMN hrm_salary_month_record.end_time IS '计薪结束日期';

COMMENT ON COLUMN hrm_salary_month_record.expected_pay_salary IS '应发工资';

COMMENT ON COLUMN hrm_salary_month_record.personal_tax IS '个人所得税';

COMMENT ON COLUMN hrm_salary_month_record.real_pay_salary IS '实发工资';

COMMENT ON COLUMN hrm_salary_month_record.personal_insurance_amount IS '个人社保金额';

COMMENT ON COLUMN hrm_salary_month_record.personal_provident_fund_amount IS '个人公积金金额';

COMMENT ON COLUMN hrm_salary_month_record.corporate_insurance_amount IS '公司社保金额';

COMMENT ON COLUMN hrm_salary_month_record.corporate_provident_fund_amount IS '公司公积金金额';

COMMENT ON COLUMN hrm_salary_month_record.option_headers IS '薪资项表头快照';

COMMENT ON COLUMN hrm_salary_month_record.status IS '状态 枚举';

COMMENT ON COLUMN hrm_salary_month_record.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_month_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_month_record.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_month_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_month_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_month_record IS 'HRM 月度工资表';

COMMENT ON COLUMN hrm_salary_option.id IS '薪资项编号';

COMMENT ON COLUMN hrm_salary_option.template_id IS '标准薪资项目录编号（可选）';

COMMENT ON COLUMN hrm_salary_option.code IS '薪资项编码';

COMMENT ON COLUMN hrm_salary_option.parent_code IS '父薪资项编码';

COMMENT ON COLUMN hrm_salary_option.name IS '薪资项名称';

COMMENT ON COLUMN hrm_salary_option.remark IS '备注';

COMMENT ON COLUMN hrm_salary_option.system_flag IS '是否系统默认项';

COMMENT ON COLUMN hrm_salary_option.type IS '薪资项类型 枚举';

COMMENT ON COLUMN hrm_salary_option.visible IS '是否显示';

COMMENT ON COLUMN hrm_salary_option.enabled IS '是否启用';

COMMENT ON COLUMN hrm_salary_option.tax_enabled IS '是否计税';

COMMENT ON COLUMN hrm_salary_option.calculate_enabled IS '是否参与计算';

COMMENT ON COLUMN hrm_salary_option.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_option.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_option.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_option.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_option.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_option IS 'HRM 工资表薪资项';

COMMENT ON COLUMN hrm_salary_option_template.id IS '标准薪资项编号';

COMMENT ON COLUMN hrm_salary_option_template.code IS '薪资项编码';

COMMENT ON COLUMN hrm_salary_option_template.parent_code IS '父薪资项编码';

COMMENT ON COLUMN hrm_salary_option_template.name IS '薪资项名称';

COMMENT ON COLUMN hrm_salary_option_template.remark IS '备注';

COMMENT ON COLUMN hrm_salary_option_template.system_flag IS '是否系统默认项';

COMMENT ON COLUMN hrm_salary_option_template.type IS '薪资项类型 枚举';

COMMENT ON COLUMN hrm_salary_option_template.visible IS '是否显示';

COMMENT ON COLUMN hrm_salary_option_template.tax_enabled IS '是否计税';

COMMENT ON COLUMN hrm_salary_option_template.calculate_enabled IS '是否参与计算';

COMMENT ON COLUMN hrm_salary_option_template.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_option_template.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_option_template.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_option_template.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_option_template.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_option_template IS 'HRM 标准薪资项目录';

COMMENT ON COLUMN hrm_salary_slip.id IS '编号';

COMMENT ON COLUMN hrm_salary_slip.send_record_id IS '工资条发放记录编号';

COMMENT ON COLUMN hrm_salary_slip.month_employee_record_id IS '员工月度工资记录编号';

COMMENT ON COLUMN hrm_salary_slip.employee_id IS '员工编号';

COMMENT ON COLUMN hrm_salary_slip.year IS '年份';

COMMENT ON COLUMN hrm_salary_slip.month IS '月份';

COMMENT ON COLUMN hrm_salary_slip.read_status IS '阅读状态 枚举';

COMMENT ON COLUMN hrm_salary_slip.real_pay_salary IS '实发工资';

COMMENT ON COLUMN hrm_salary_slip.remark IS '备注';

COMMENT ON COLUMN hrm_salary_slip.options IS '工资条项 工资条项随员工工资条整体维护，通过  表达父子关系';

COMMENT ON COLUMN hrm_salary_slip.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_slip.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_slip.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_slip.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_slip.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_slip IS 'HRM 工资条';

COMMENT ON COLUMN hrm_salary_slip_send_record.id IS '编号';

COMMENT ON COLUMN hrm_salary_slip_send_record.month_record_id IS '月度工资表编号';

COMMENT ON COLUMN hrm_salary_slip_send_record.employee_count IS '工资表总人数';

COMMENT ON COLUMN hrm_salary_slip_send_record.send_employee_count IS '本批次发放人数';

COMMENT ON COLUMN hrm_salary_slip_send_record.year IS '年份';

COMMENT ON COLUMN hrm_salary_slip_send_record.month IS '月份';

COMMENT ON COLUMN hrm_salary_slip_send_record.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_slip_send_record.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_slip_send_record.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_slip_send_record.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_slip_send_record.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_slip_send_record IS 'HRM 工资条发放记录 DO 同一月度工资表可以分多批次发放工资条，因此发放记录保持独立实体。';

COMMENT ON COLUMN hrm_salary_slip_template.id IS '编号';

COMMENT ON COLUMN hrm_salary_slip_template.name IS '模板名称';

COMMENT ON COLUMN hrm_salary_slip_template.hide_empty IS '是否隐藏空项';

COMMENT ON COLUMN hrm_salary_slip_template.default_status IS '是否默认模板';

COMMENT ON COLUMN hrm_salary_slip_template.options IS '工资条模板项 模板项随工资条模板整体维护，通过  表达父子关系';

COMMENT ON COLUMN hrm_salary_slip_template.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_slip_template.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_slip_template.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_slip_template.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_slip_template.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_slip_template IS 'HRM 工资条模板 DO 平台级默认模板保存在 {@link cn.iocoder.yudao.framework.te';

COMMENT ON COLUMN hrm_salary_tax_rule.name IS '计税规则名称';

COMMENT ON COLUMN hrm_salary_tax_rule.type IS '计税类型 枚举';

COMMENT ON COLUMN hrm_salary_tax_rule.tax_enabled IS '是否计税';

COMMENT ON COLUMN hrm_salary_tax_rule.threshold IS '起征阈值';

COMMENT ON COLUMN hrm_salary_tax_rule.decimal_scale IS '小数位数';

COMMENT ON COLUMN hrm_salary_tax_rule.cycle_type IS '计税周期类型 枚举';

COMMENT ON COLUMN hrm_salary_tax_rule.creator IS '创建者';

COMMENT ON COLUMN hrm_salary_tax_rule.create_time IS '创建时间';

COMMENT ON COLUMN hrm_salary_tax_rule.updater IS '更新者';

COMMENT ON COLUMN hrm_salary_tax_rule.update_time IS '更新时间';

COMMENT ON COLUMN hrm_salary_tax_rule.deleted IS '是否删除';

COMMENT ON TABLE hrm_salary_tax_rule IS 'HRM 计税规则';

COMMENT ON COLUMN milestones.type IS 'birthday/anniversary/promotion/probation';

COMMENT ON TABLE milestones IS '个人里程碑';

COMMENT ON COLUMN notifications.user_id IS '接收人';

COMMENT ON COLUMN notifications.category IS 'todo / milestone / compliance / corp';

COMMENT ON COLUMN notifications.type IS '具体类型，如 report_due';

COMMENT ON COLUMN notifications.link IS '一键跳转到对应模块';

COMMENT ON COLUMN notifications.read_flag IS '是否已读';

COMMENT ON COLUMN notifications.channel IS 'site / wecom';

COMMENT ON COLUMN notifications.dedup_key IS '去重键，同一用户同键只保留一条';

COMMENT ON TABLE notifications IS '我的提醒';

COMMENT ON TABLE notify_preferences IS '提醒偏好';

COMMENT ON COLUMN oa_announcement.id IS '公告编号';

COMMENT ON COLUMN oa_announcement.type IS '公告类型';

COMMENT ON COLUMN oa_announcement.priority IS '优先级';

COMMENT ON COLUMN oa_announcement.title IS '标题';

COMMENT ON COLUMN oa_announcement.content IS '内容';

COMMENT ON COLUMN oa_announcement.url IS '相关链接';

COMMENT ON COLUMN oa_announcement.top IS '是否置顶';

COMMENT ON COLUMN oa_announcement.creator IS '创建者';

COMMENT ON COLUMN oa_announcement.create_time IS '创建时间';

COMMENT ON COLUMN oa_announcement.updater IS '更新者';

COMMENT ON COLUMN oa_announcement.update_time IS '更新时间';

COMMENT ON COLUMN oa_announcement.deleted IS '是否删除';

COMMENT ON TABLE oa_announcement IS 'OA 公告表';

COMMENT ON COLUMN oa_announcement_receiver.id IS '接收关系编号';

COMMENT ON COLUMN oa_announcement_receiver.announcement_id IS '公告编号';

COMMENT ON COLUMN oa_announcement_receiver.receiver_user_id IS '接收人用户编号';

COMMENT ON COLUMN oa_announcement_receiver.read_status IS '是否已读';

COMMENT ON COLUMN oa_announcement_receiver.creator IS '创建者';

COMMENT ON COLUMN oa_announcement_receiver.create_time IS '创建时间';

COMMENT ON COLUMN oa_announcement_receiver.updater IS '更新者';

COMMENT ON COLUMN oa_announcement_receiver.update_time IS '更新时间';

COMMENT ON COLUMN oa_announcement_receiver.deleted IS '是否删除';

COMMENT ON TABLE oa_announcement_receiver IS 'OA 公告接收关系表';

COMMENT ON COLUMN oa_attendance.id IS '考勤编号';

COMMENT ON COLUMN oa_attendance.user_id IS '用户编号';

COMMENT ON COLUMN oa_attendance.type IS '考勤类型';

COMMENT ON COLUMN oa_attendance.status IS '考勤状态';

COMMENT ON COLUMN oa_attendance.attendance_time IS '考勤时间';

COMMENT ON COLUMN oa_attendance.attendance_ip IS '考勤 IP';

COMMENT ON COLUMN oa_attendance.remark IS '考勤备注';

COMMENT ON COLUMN oa_attendance.creator IS '创建者';

COMMENT ON COLUMN oa_attendance.create_time IS '创建时间';

COMMENT ON COLUMN oa_attendance.updater IS '更新者';

COMMENT ON COLUMN oa_attendance.update_time IS '更新时间';

COMMENT ON COLUMN oa_attendance.deleted IS '是否删除';

COMMENT ON TABLE oa_attendance IS 'OA 考勤表';

COMMENT ON COLUMN oa_leave_apply.id IS '编号';

COMMENT ON COLUMN oa_leave_apply.title IS '申请标题';

COMMENT ON COLUMN oa_leave_apply.urgency IS '紧急程度';

COMMENT ON COLUMN oa_leave_apply.type IS '请假类型';

COMMENT ON COLUMN oa_leave_apply.start_time IS '请假开始时间';

COMMENT ON COLUMN oa_leave_apply.end_time IS '请假结束时间';

COMMENT ON COLUMN oa_leave_apply.days IS '请假天数';

COMMENT ON COLUMN oa_leave_apply.reason IS '申请原因';

COMMENT ON COLUMN oa_leave_apply.approvers IS '自选审批人的员工编号 JSON 数组';

COMMENT ON COLUMN oa_leave_apply.status IS '审批状态，未开始表示尚未提交的草稿';

COMMENT ON COLUMN oa_leave_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_leave_apply.wecom_approval_no IS '企微审批单号 sp_no';

COMMENT ON COLUMN oa_leave_apply.wecom_request_id IS '提交企微的幂等请求号';

COMMENT ON COLUMN oa_leave_apply.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_leave_apply.creator IS '创建者';

COMMENT ON COLUMN oa_leave_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_leave_apply.updater IS '更新者';

COMMENT ON COLUMN oa_leave_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_leave_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_leave_apply IS 'OA 请假申请表';

COMMENT ON COLUMN oa_meeting_room.id IS '编号';

COMMENT ON COLUMN oa_meeting_room.name IS '会议室名称';

COMMENT ON COLUMN oa_meeting_room.location IS '会议室位置';

COMMENT ON COLUMN oa_meeting_room.type IS '会议室类型';

COMMENT ON COLUMN oa_meeting_room.manager_user_id IS '负责人用户编号';

COMMENT ON COLUMN oa_meeting_room.status IS '可用状态';

COMMENT ON COLUMN oa_meeting_room.pic_url IS '会议室图片 URL';

COMMENT ON COLUMN oa_meeting_room.seat_count IS '坐席数';

COMMENT ON COLUMN oa_meeting_room.equipments IS '设备列表';

COMMENT ON COLUMN oa_meeting_room.allow_booking IS '是否允许预定';

COMMENT ON COLUMN oa_meeting_room.need_approval IS '预定是否需要审批';

COMMENT ON COLUMN oa_meeting_room.booking_scope IS '可预定范围';

COMMENT ON COLUMN oa_meeting_room.booking_user_ids IS '可预定用户编号列表';

COMMENT ON COLUMN oa_meeting_room.sort IS '显示顺序';

COMMENT ON COLUMN oa_meeting_room.remark IS '备注';

COMMENT ON COLUMN oa_meeting_room.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_meeting_room.creator IS '创建者';

COMMENT ON COLUMN oa_meeting_room.create_time IS '创建时间';

COMMENT ON COLUMN oa_meeting_room.updater IS '更新者';

COMMENT ON COLUMN oa_meeting_room.update_time IS '更新时间';

COMMENT ON COLUMN oa_meeting_room.deleted IS '是否删除';

COMMENT ON TABLE oa_meeting_room IS 'OA 会议室表';

COMMENT ON COLUMN oa_meeting_room_booking.id IS '编号';

COMMENT ON COLUMN oa_meeting_room_booking.no IS '预定申请单号';

COMMENT ON COLUMN oa_meeting_room_booking.room_id IS '会议室编号';

COMMENT ON COLUMN oa_meeting_room_booking.title IS '会议主题';

COMMENT ON COLUMN oa_meeting_room_booking.start_time IS '会议开始时间';

COMMENT ON COLUMN oa_meeting_room_booking.end_time IS '会议结束时间';

COMMENT ON COLUMN oa_meeting_room_booking.moderator_user_id IS '主持人用户编号';

COMMENT ON COLUMN oa_meeting_room_booking.attendee_user_ids IS '参会用户编号列表';

COMMENT ON COLUMN oa_meeting_room_booking.reminder_type IS '会议提醒类型';

COMMENT ON COLUMN oa_meeting_room_booking.reminded IS '是否已发送会议开始提醒';

COMMENT ON COLUMN oa_meeting_room_booking.description IS '会议说明';

COMMENT ON COLUMN oa_meeting_room_booking.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_meeting_room_booking.status IS '审批状态，无需审批时提交后直接通过';

COMMENT ON COLUMN oa_meeting_room_booking.process_instance_id IS 'BPM 流程实例编号，无需审批时为空';

COMMENT ON COLUMN oa_meeting_room_booking.need_approval IS '提交时是否需要审批';

COMMENT ON COLUMN oa_meeting_room_booking.use_status IS '使用状态';

COMMENT ON COLUMN oa_meeting_room_booking.remark IS '申请备注';

COMMENT ON COLUMN oa_meeting_room_booking.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_meeting_room_booking.creator IS '创建者';

COMMENT ON COLUMN oa_meeting_room_booking.create_time IS '创建时间';

COMMENT ON COLUMN oa_meeting_room_booking.updater IS '更新者';

COMMENT ON COLUMN oa_meeting_room_booking.update_time IS '更新时间';

COMMENT ON COLUMN oa_meeting_room_booking.deleted IS '是否删除';

COMMENT ON TABLE oa_meeting_room_booking IS 'OA 会议室预定表';

COMMENT ON COLUMN oa_official_doc_receive.id IS '编号';

COMMENT ON COLUMN oa_official_doc_receive.no IS '单据编号';

COMMENT ON COLUMN oa_official_doc_receive.send_id IS '来源发文编号，手工新增收文时为空';

COMMENT ON COLUMN oa_official_doc_receive.receive_type IS '收文类型';

COMMENT ON COLUMN oa_official_doc_receive.title IS '标题';

COMMENT ON COLUMN oa_official_doc_receive.document_no IS '来文字号';

COMMENT ON COLUMN oa_official_doc_receive.secrecy_level IS '密级';

COMMENT ON COLUMN oa_official_doc_receive.urgency_level IS '紧急程度';

COMMENT ON COLUMN oa_official_doc_receive.receive_time IS '收文时间';

COMMENT ON COLUMN oa_official_doc_receive.receive_dept_id IS '收文部门编号';

COMMENT ON COLUMN oa_official_doc_receive.handler_user_id IS '主办人用户编号';

COMMENT ON COLUMN oa_official_doc_receive.instruction IS '领导批示';

COMMENT ON COLUMN oa_official_doc_receive.result IS '办理结果';

COMMENT ON COLUMN oa_official_doc_receive.deadline_time IS '办理期限';

COMMENT ON COLUMN oa_official_doc_receive.summary IS '内容摘要';

COMMENT ON COLUMN oa_official_doc_receive.remark IS '备注';

COMMENT ON COLUMN oa_official_doc_receive.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_official_doc_receive.formal_file_url IS '正式公文地址';

COMMENT ON COLUMN oa_official_doc_receive.handle_status IS '办理状态';

COMMENT ON COLUMN oa_official_doc_receive.status IS '审批状态';

COMMENT ON COLUMN oa_official_doc_receive.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_official_doc_receive.creator IS '创建者';

COMMENT ON COLUMN oa_official_doc_receive.create_time IS '创建时间';

COMMENT ON COLUMN oa_official_doc_receive.updater IS '更新者';

COMMENT ON COLUMN oa_official_doc_receive.update_time IS '更新时间';

COMMENT ON COLUMN oa_official_doc_receive.deleted IS '是否删除';

COMMENT ON TABLE oa_official_doc_receive IS 'OA 公文收文表';

COMMENT ON COLUMN oa_official_doc_send.id IS '编号';

COMMENT ON COLUMN oa_official_doc_send.no IS '单据编号';

COMMENT ON COLUMN oa_official_doc_send.template_id IS '套红模板编号';

COMMENT ON COLUMN oa_official_doc_send.title IS '标题';

COMMENT ON COLUMN oa_official_doc_send.no_prefix IS '发文字号前缀';

COMMENT ON COLUMN oa_official_doc_send.year IS '发文年度';

COMMENT ON COLUMN oa_official_doc_send.sequence IS '发文序号';

COMMENT ON COLUMN oa_official_doc_send.document_no IS '公文文号';

COMMENT ON COLUMN oa_official_doc_send.secrecy_level IS '密级';

COMMENT ON COLUMN oa_official_doc_send.urgency_level IS '紧急程度';

COMMENT ON COLUMN oa_official_doc_send.disclosure_type IS '公开类别';

COMMENT ON COLUMN oa_official_doc_send.issue_time IS '发文时间';

COMMENT ON COLUMN oa_official_doc_send.send_dept_id IS '发文部门编号';

COMMENT ON COLUMN oa_official_doc_send.main_dept_ids IS '主送部门编号列表';

COMMENT ON COLUMN oa_official_doc_send.copy_dept_ids IS '抄送部门编号列表';

COMMENT ON COLUMN oa_official_doc_send.signer_user_id IS '签发人用户编号';

COMMENT ON COLUMN oa_official_doc_send.content IS '正文';

COMMENT ON COLUMN oa_official_doc_send.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_official_doc_send.formal_file_url IS '正式公文地址';

COMMENT ON COLUMN oa_official_doc_send.remark IS '附注';

COMMENT ON COLUMN oa_official_doc_send.status IS '审批状态';

COMMENT ON COLUMN oa_official_doc_send.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_official_doc_send.creator IS '创建者';

COMMENT ON COLUMN oa_official_doc_send.create_time IS '创建时间';

COMMENT ON COLUMN oa_official_doc_send.updater IS '更新者';

COMMENT ON COLUMN oa_official_doc_send.update_time IS '更新时间';

COMMENT ON COLUMN oa_official_doc_send.deleted IS '是否删除';

COMMENT ON TABLE oa_official_doc_send IS 'OA 公文发文表';

COMMENT ON COLUMN oa_official_doc_template.id IS '编号';

COMMENT ON COLUMN oa_official_doc_template.name IS '模板名称';

COMMENT ON COLUMN oa_official_doc_template.authority_name IS '红头名称';

COMMENT ON COLUMN oa_official_doc_template.font_size IS '红头名称字号';

COMMENT ON COLUMN oa_official_doc_template.no_prefix IS '发文字号前缀';

COMMENT ON COLUMN oa_official_doc_template.seal_pic_url IS '印章图片地址';

COMMENT ON COLUMN oa_official_doc_template.separator_type IS '分隔线类型';

COMMENT ON COLUMN oa_official_doc_template.status IS '状态';

COMMENT ON COLUMN oa_official_doc_template.sort IS '显示顺序';

COMMENT ON COLUMN oa_official_doc_template.remark IS '备注';

COMMENT ON COLUMN oa_official_doc_template.creator IS '创建者';

COMMENT ON COLUMN oa_official_doc_template.create_time IS '创建时间';

COMMENT ON COLUMN oa_official_doc_template.updater IS '更新者';

COMMENT ON COLUMN oa_official_doc_template.update_time IS '更新时间';

COMMENT ON COLUMN oa_official_doc_template.deleted IS '是否删除';

COMMENT ON TABLE oa_official_doc_template IS 'OA 套红模板表';

COMMENT ON COLUMN oa_overtime_apply.id IS '编号';

COMMENT ON COLUMN oa_overtime_apply.title IS '申请标题';

COMMENT ON COLUMN oa_overtime_apply.urgency IS '紧急程度';

COMMENT ON COLUMN oa_overtime_apply.type IS '加班类型';

COMMENT ON COLUMN oa_overtime_apply.start_time IS '加班开始时间';

COMMENT ON COLUMN oa_overtime_apply.end_time IS '加班结束时间';

COMMENT ON COLUMN oa_overtime_apply.days IS '加班天数';

COMMENT ON COLUMN oa_overtime_apply.reason IS '申请原因';

COMMENT ON COLUMN oa_overtime_apply.status IS '审批状态，未开始表示尚未提交的草稿';

COMMENT ON COLUMN oa_overtime_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_overtime_apply.creator IS '创建者';

COMMENT ON COLUMN oa_overtime_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_overtime_apply.updater IS '更新者';

COMMENT ON COLUMN oa_overtime_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_overtime_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_overtime_apply IS 'OA 加班申请表';

COMMENT ON COLUMN oa_regular_apply.id IS '编号';

COMMENT ON COLUMN oa_regular_apply.title IS '申请标题';

COMMENT ON COLUMN oa_regular_apply.urgency IS '紧急程度';

COMMENT ON COLUMN oa_regular_apply.start_time IS '试用开始时间';

COMMENT ON COLUMN oa_regular_apply.end_time IS '试用结束时间';

COMMENT ON COLUMN oa_regular_apply.days IS '试用天数';

COMMENT ON COLUMN oa_regular_apply.experience IS '试用期或实习期心得体会';

COMMENT ON COLUMN oa_regular_apply.understanding IS '对本岗位职责要求的理解';

COMMENT ON COLUMN oa_regular_apply.growth IS '试用期或实习期的成长';

COMMENT ON COLUMN oa_regular_apply.deficiency IS '目前存在的不足';

COMMENT ON COLUMN oa_regular_apply.improvement IS '本岗位工作改进计划';

COMMENT ON COLUMN oa_regular_apply.suggestion IS '对公司产品的意见及建议';

COMMENT ON COLUMN oa_regular_apply.status IS '审批状态，未开始表示尚未提交的草稿';

COMMENT ON COLUMN oa_regular_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_regular_apply.creator IS '创建者';

COMMENT ON COLUMN oa_regular_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_regular_apply.updater IS '更新者';

COMMENT ON COLUMN oa_regular_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_regular_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_regular_apply IS 'OA 转正申请表';

COMMENT ON COLUMN oa_reimbursement.id IS '编号';

COMMENT ON COLUMN oa_reimbursement.title IS '申请标题';

COMMENT ON COLUMN oa_reimbursement.urgency IS '紧急程度';

COMMENT ON COLUMN oa_reimbursement.reason IS '申请原因';

COMMENT ON COLUMN oa_reimbursement.witness_user_id IS '证明人用户编号';

COMMENT ON COLUMN oa_reimbursement.customer_name IS '相关客户名称';

COMMENT ON COLUMN oa_reimbursement.payment_method IS '报销方式';

COMMENT ON COLUMN oa_reimbursement.invoice_count IS '票据总数';

COMMENT ON COLUMN oa_reimbursement.total_price IS '报销总金额，单位：元';

COMMENT ON COLUMN oa_reimbursement.status IS '审批状态，未开始表示尚未提交的草稿';

COMMENT ON COLUMN oa_reimbursement.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_reimbursement.items IS '报销明细';

COMMENT ON COLUMN oa_reimbursement.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_reimbursement.creator IS '创建者';

COMMENT ON COLUMN oa_reimbursement.create_time IS '创建时间';

COMMENT ON COLUMN oa_reimbursement.updater IS '更新者';

COMMENT ON COLUMN oa_reimbursement.update_time IS '更新时间';

COMMENT ON COLUMN oa_reimbursement.deleted IS '是否删除';

COMMENT ON TABLE oa_reimbursement IS 'OA 费用报销表';

COMMENT ON COLUMN oa_resign_apply.id IS '编号';

COMMENT ON COLUMN oa_resign_apply.title IS '申请标题';

COMMENT ON COLUMN oa_resign_apply.urgency IS '紧急程度';

COMMENT ON COLUMN oa_resign_apply.reason IS '申请原因';

COMMENT ON COLUMN oa_resign_apply.handover_user_id IS '工作交接人用户编号';

COMMENT ON COLUMN oa_resign_apply.unfinished_work IS '未完成事宜';

COMMENT ON COLUMN oa_resign_apply.has_pending_reimbursement IS '是否有未完成的费用报销';

COMMENT ON COLUMN oa_resign_apply.suggestion IS '申请人的意见及建议';

COMMENT ON COLUMN oa_resign_apply.status IS '审批状态，未开始表示尚未提交的草稿';

COMMENT ON COLUMN oa_resign_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_resign_apply.creator IS '创建者';

COMMENT ON COLUMN oa_resign_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_resign_apply.updater IS '更新者';

COMMENT ON COLUMN oa_resign_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_resign_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_resign_apply IS 'OA 离职申请表';

COMMENT ON COLUMN oa_schedule.id IS '日程编号';

COMMENT ON COLUMN oa_schedule.type IS '日程类型';

COMMENT ON COLUMN oa_schedule.priority IS '优先级';

COMMENT ON COLUMN oa_schedule.title IS '标题';

COMMENT ON COLUMN oa_schedule.description IS '描述';

COMMENT ON COLUMN oa_schedule.start_time IS '开始时间';

COMMENT ON COLUMN oa_schedule.end_time IS '结束时间';

COMMENT ON COLUMN oa_schedule.remind IS '是否提醒';

COMMENT ON COLUMN oa_schedule.reminded IS '是否已提醒';

COMMENT ON COLUMN oa_schedule.creator IS '创建者';

COMMENT ON COLUMN oa_schedule.create_time IS '创建时间';

COMMENT ON COLUMN oa_schedule.updater IS '更新者';

COMMENT ON COLUMN oa_schedule.update_time IS '更新时间';

COMMENT ON COLUMN oa_schedule.deleted IS '是否删除';

COMMENT ON TABLE oa_schedule IS 'OA 日程表';

COMMENT ON COLUMN oa_schedule_participant.id IS '参与记录编号';

COMMENT ON COLUMN oa_schedule_participant.schedule_id IS '日程编号';

COMMENT ON COLUMN oa_schedule_participant.user_id IS '参与人用户编号';

COMMENT ON COLUMN oa_schedule_participant.read_status IS '是否已读';

COMMENT ON COLUMN oa_schedule_participant.read_time IS '首次阅读时间';

COMMENT ON COLUMN oa_schedule_participant.creator IS '创建者';

COMMENT ON COLUMN oa_schedule_participant.create_time IS '创建时间';

COMMENT ON COLUMN oa_schedule_participant.updater IS '更新者';

COMMENT ON COLUMN oa_schedule_participant.update_time IS '更新时间';

COMMENT ON COLUMN oa_schedule_participant.deleted IS '是否删除';

COMMENT ON TABLE oa_schedule_participant IS 'OA 日程参与人表';

COMMENT ON COLUMN oa_seal.id IS '编号';

COMMENT ON COLUMN oa_seal.dept_id IS '所属部门编号';

COMMENT ON COLUMN oa_seal.no IS '印章编号';

COMMENT ON COLUMN oa_seal.name IS '印章名称';

COMMENT ON COLUMN oa_seal.type IS '印章类型';

COMMENT ON COLUMN oa_seal.category IS '印章分类';

COMMENT ON COLUMN oa_seal.keeper_user_id IS '保管人用户编号';

COMMENT ON COLUMN oa_seal.keeper_dept_id IS '保管部门编号';

COMMENT ON COLUMN oa_seal.status IS '印章台账状态';

COMMENT ON COLUMN oa_seal.purchase_time IS '购买时间';

COMMENT ON COLUMN oa_seal.enable_time IS '启用时间';

COMMENT ON COLUMN oa_seal.disable_time IS '停用时间';

COMMENT ON COLUMN oa_seal.pic_url IS '印章照片地址';

COMMENT ON COLUMN oa_seal.sort IS '显示顺序';

COMMENT ON COLUMN oa_seal.remark IS '备注';

COMMENT ON COLUMN oa_seal.creator IS '创建者';

COMMENT ON COLUMN oa_seal.create_time IS '创建时间';

COMMENT ON COLUMN oa_seal.updater IS '更新者';

COMMENT ON COLUMN oa_seal.update_time IS '更新时间';

COMMENT ON COLUMN oa_seal.deleted IS '是否删除';

COMMENT ON TABLE oa_seal IS 'OA 印章表';

COMMENT ON COLUMN oa_seal_apply.id IS '编号';

COMMENT ON COLUMN oa_seal_apply.no IS '申请单号';

COMMENT ON COLUMN oa_seal_apply.user_id IS '申请人用户编号';

COMMENT ON COLUMN oa_seal_apply.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_seal_apply.seal_id IS '印章编号';

COMMENT ON COLUMN oa_seal_apply.seal_no IS '印章编号快照';

COMMENT ON COLUMN oa_seal_apply.seal_name IS '印章名称快照';

COMMENT ON COLUMN oa_seal_apply.seal_type IS '印章类型快照';

COMMENT ON COLUMN oa_seal_apply.keeper_dept_id IS '保管部门编号快照';

COMMENT ON COLUMN oa_seal_apply.keeper_user_id IS '保管人用户编号快照';

COMMENT ON COLUMN oa_seal_apply.reason IS '用印事由';

COMMENT ON COLUMN oa_seal_apply.type IS '用印类型';

COMMENT ON COLUMN oa_seal_apply.mode IS '用印方式';

COMMENT ON COLUMN oa_seal_apply.document_title IS '文件标题';

COMMENT ON COLUMN oa_seal_apply.document_type IS '文件类型';

COMMENT ON COLUMN oa_seal_apply.document_count IS '文件份数';

COMMENT ON COLUMN oa_seal_apply.contract_price IS '合同金额';

COMMENT ON COLUMN oa_seal_apply.contract_party IS '合同对方';

COMMENT ON COLUMN oa_seal_apply.expected_use_time IS '预计用印时间';

COMMENT ON COLUMN oa_seal_apply.actual_use_time IS '实际用印时间';

COMMENT ON COLUMN oa_seal_apply.expected_return_time IS '预计归还时间';

COMMENT ON COLUMN oa_seal_apply.actual_return_time IS '实际归还时间';

COMMENT ON COLUMN oa_seal_apply.use_status IS '用印业务状态';

COMMENT ON COLUMN oa_seal_apply.status IS '审批状态，取 BPM 流程实例状态';

COMMENT ON COLUMN oa_seal_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_seal_apply.urgent IS '是否紧急';

COMMENT ON COLUMN oa_seal_apply.remark IS '备注';

COMMENT ON COLUMN oa_seal_apply.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_seal_apply.creator IS '创建者';

COMMENT ON COLUMN oa_seal_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_seal_apply.updater IS '更新者';

COMMENT ON COLUMN oa_seal_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_seal_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_seal_apply IS 'OA 用印申请表';

COMMENT ON COLUMN oa_supply_apply.id IS '编号';

COMMENT ON COLUMN oa_supply_apply.no IS '申请单号';

COMMENT ON COLUMN oa_supply_apply.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_supply_apply.apply_time IS '领用日期';

COMMENT ON COLUMN oa_supply_apply.use_type IS '使用类型';

COMMENT ON COLUMN oa_supply_apply.pickup_method IS '领取方式';

COMMENT ON COLUMN oa_supply_apply.reason IS '申请事由';

COMMENT ON COLUMN oa_supply_apply.status IS '审批状态';

COMMENT ON COLUMN oa_supply_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_supply_apply.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_supply_apply.remark IS '备注';

COMMENT ON COLUMN oa_supply_apply.creator IS '创建者';

COMMENT ON COLUMN oa_supply_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_supply_apply.updater IS '更新者';

COMMENT ON COLUMN oa_supply_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_supply_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_supply_apply IS 'OA 用品领用申请表';

COMMENT ON COLUMN oa_supply_apply_item.id IS '编号';

COMMENT ON COLUMN oa_supply_apply_item.apply_id IS '领用申请编号';

COMMENT ON COLUMN oa_supply_apply_item.item_id IS '办公用品编号';

COMMENT ON COLUMN oa_supply_apply_item.item_name IS '物品名称快照';

COMMENT ON COLUMN oa_supply_apply_item.model IS '规格型号快照';

COMMENT ON COLUMN oa_supply_apply_item.unit IS '计量单位快照';

COMMENT ON COLUMN oa_supply_apply_item.manage_type IS '管理类型快照';

COMMENT ON COLUMN oa_supply_apply_item.apply_quantity IS '申请数量';

COMMENT ON COLUMN oa_supply_apply_item.issued_quantity IS '实发数量';

COMMENT ON COLUMN oa_supply_apply_item.returned_quantity IS '累计归还数量';

COMMENT ON COLUMN oa_supply_apply_item.status IS '明细状态';

COMMENT ON COLUMN oa_supply_apply_item.issue_user_id IS '发放人用户编号';

COMMENT ON COLUMN oa_supply_apply_item.issue_time IS '发放时间';

COMMENT ON COLUMN oa_supply_apply_item.issue_remark IS '发放备注';

COMMENT ON COLUMN oa_supply_apply_item.return_remark IS '归还备注';

COMMENT ON COLUMN oa_supply_apply_item.creator IS '创建者';

COMMENT ON COLUMN oa_supply_apply_item.create_time IS '创建时间';

COMMENT ON COLUMN oa_supply_apply_item.updater IS '更新者';

COMMENT ON COLUMN oa_supply_apply_item.update_time IS '更新时间';

COMMENT ON COLUMN oa_supply_apply_item.deleted IS '是否删除';

COMMENT ON TABLE oa_supply_apply_item IS 'OA 用品申请明细表';

COMMENT ON COLUMN oa_supply_item.id IS '编号';

COMMENT ON COLUMN oa_supply_item.dept_id IS '所属部门编号';

COMMENT ON COLUMN oa_supply_item.name IS '物品名称';

COMMENT ON COLUMN oa_supply_item.no IS '物品编码';

COMMENT ON COLUMN oa_supply_item.category IS '类别';

COMMENT ON COLUMN oa_supply_item.manage_type IS '管理类型';

COMMENT ON COLUMN oa_supply_item.model IS '规格型号';

COMMENT ON COLUMN oa_supply_item.unit IS '计量单位';

COMMENT ON COLUMN oa_supply_item.reference_price IS '参考单价';

COMMENT ON COLUMN oa_supply_item.stock_quantity IS '库存数量';

COMMENT ON COLUMN oa_supply_item.min_stock_quantity IS '最低库存预警';

COMMENT ON COLUMN oa_supply_item.pic_url IS '物品图片 URL';

COMMENT ON COLUMN oa_supply_item.status IS '状态';

COMMENT ON COLUMN oa_supply_item.sort IS '显示顺序';

COMMENT ON COLUMN oa_supply_item.remark IS '备注';

COMMENT ON COLUMN oa_supply_item.creator IS '创建者';

COMMENT ON COLUMN oa_supply_item.create_time IS '创建时间';

COMMENT ON COLUMN oa_supply_item.updater IS '更新者';

COMMENT ON COLUMN oa_supply_item.update_time IS '更新时间';

COMMENT ON COLUMN oa_supply_item.deleted IS '是否删除';

COMMENT ON TABLE oa_supply_item IS 'OA 办公用品表';

COMMENT ON COLUMN oa_task.id IS '任务编号';

COMMENT ON COLUMN oa_task.type IS '任务类型';

COMMENT ON COLUMN oa_task.status IS '总体状态';

COMMENT ON COLUMN oa_task.title IS '标题';

COMMENT ON COLUMN oa_task.description IS '任务描述';

COMMENT ON COLUMN oa_task.comment IS '任务评价';

COMMENT ON COLUMN oa_task.start_time IS '开始时间';

COMMENT ON COLUMN oa_task.end_time IS '结束时间';

COMMENT ON COLUMN oa_task.publish_time IS '业务发布时间，新增及编辑任务时刷新';

COMMENT ON COLUMN oa_task.top IS '是否置顶';

COMMENT ON COLUMN oa_task.canceled IS '是否取消';

COMMENT ON COLUMN oa_task.creator IS '创建者';

COMMENT ON COLUMN oa_task.create_time IS '创建时间';

COMMENT ON COLUMN oa_task.updater IS '更新者';

COMMENT ON COLUMN oa_task.update_time IS '更新时间';

COMMENT ON COLUMN oa_task.deleted IS '是否删除';

COMMENT ON TABLE oa_task IS 'OA 任务表';

COMMENT ON COLUMN oa_task_log.id IS '编号';

COMMENT ON COLUMN oa_task_log.task_id IS '任务编号';

COMMENT ON COLUMN oa_task_log.user_id IS '反馈人用户编号';

COMMENT ON COLUMN oa_task_log.status IS '变更后的状态';

COMMENT ON COLUMN oa_task_log.content IS '反馈内容';

COMMENT ON COLUMN oa_task_log.creator IS '创建者';

COMMENT ON COLUMN oa_task_log.create_time IS '创建时间';

COMMENT ON COLUMN oa_task_log.updater IS '更新者';

COMMENT ON COLUMN oa_task_log.update_time IS '更新时间';

COMMENT ON COLUMN oa_task_log.deleted IS '是否删除';

COMMENT ON TABLE oa_task_log IS 'OA 任务反馈日志表';

COMMENT ON COLUMN oa_task_receiver.id IS '编号';

COMMENT ON COLUMN oa_task_receiver.task_id IS '任务编号';

COMMENT ON COLUMN oa_task_receiver.user_id IS '接收人用户编号';

COMMENT ON COLUMN oa_task_receiver.status IS '任务状态';

COMMENT ON COLUMN oa_task_receiver.creator IS '创建者';

COMMENT ON COLUMN oa_task_receiver.create_time IS '创建时间';

COMMENT ON COLUMN oa_task_receiver.updater IS '更新者';

COMMENT ON COLUMN oa_task_receiver.update_time IS '更新时间';

COMMENT ON COLUMN oa_task_receiver.deleted IS '是否删除';

COMMENT ON TABLE oa_task_receiver IS 'OA 任务接收人表';

COMMENT ON COLUMN oa_travel_apply.id IS '编号';

COMMENT ON COLUMN oa_travel_apply.no IS '单据编号';

COMMENT ON COLUMN oa_travel_apply.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_travel_apply.reason IS '出差事由';

COMMENT ON COLUMN oa_travel_apply.start_time IS '出差开始时间';

COMMENT ON COLUMN oa_travel_apply.end_time IS '出差结束时间';

COMMENT ON COLUMN oa_travel_apply.days IS '出差天数';

COMMENT ON COLUMN oa_travel_apply.companion IS '同行人';

COMMENT ON COLUMN oa_travel_apply.estimated_price IS '预计费用，单位：元';

COMMENT ON COLUMN oa_travel_apply.reimburse_status IS '是否已报销';

COMMENT ON COLUMN oa_travel_apply.status IS '审批状态';

COMMENT ON COLUMN oa_travel_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_travel_apply.remark IS '备注';

COMMENT ON COLUMN oa_travel_apply.items IS '行程明细';

COMMENT ON COLUMN oa_travel_apply.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_travel_apply.creator IS '创建者';

COMMENT ON COLUMN oa_travel_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_travel_apply.updater IS '更新者';

COMMENT ON COLUMN oa_travel_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_travel_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_travel_apply IS 'OA 出差申请表';

COMMENT ON COLUMN oa_travel_reimbursement.id IS '编号';

COMMENT ON COLUMN oa_travel_reimbursement.no IS '单据编号';

COMMENT ON COLUMN oa_travel_reimbursement.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_travel_reimbursement.travel_apply_id IS '关联出差申请编号，未关联申请时为空';

COMMENT ON COLUMN oa_travel_reimbursement.reason IS '出差事由';

COMMENT ON COLUMN oa_travel_reimbursement.start_time IS '出差开始时间';

COMMENT ON COLUMN oa_travel_reimbursement.end_time IS '出差结束时间';

COMMENT ON COLUMN oa_travel_reimbursement.days IS '出差天数';

COMMENT ON COLUMN oa_travel_reimbursement.total_price IS '报销总金额，单位：元';

COMMENT ON COLUMN oa_travel_reimbursement.pay_status IS '是否已支付';

COMMENT ON COLUMN oa_travel_reimbursement.status IS '审批状态';

COMMENT ON COLUMN oa_travel_reimbursement.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_travel_reimbursement.remark IS '备注';

COMMENT ON COLUMN oa_travel_reimbursement.items IS '费用明细';

COMMENT ON COLUMN oa_travel_reimbursement.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_travel_reimbursement.creator IS '创建者';

COMMENT ON COLUMN oa_travel_reimbursement.create_time IS '创建时间';

COMMENT ON COLUMN oa_travel_reimbursement.updater IS '更新者';

COMMENT ON COLUMN oa_travel_reimbursement.update_time IS '更新时间';

COMMENT ON COLUMN oa_travel_reimbursement.deleted IS '是否删除';

COMMENT ON TABLE oa_travel_reimbursement IS 'OA 出差报销表';

COMMENT ON COLUMN oa_vehicle.id IS '编号';

COMMENT ON COLUMN oa_vehicle.no IS '车牌号';

COMMENT ON COLUMN oa_vehicle.name IS '车辆名称';

COMMENT ON COLUMN oa_vehicle.dept_id IS '所属部门编号';

COMMENT ON COLUMN oa_vehicle.type IS '车型';

COMMENT ON COLUMN oa_vehicle.category IS '车辆分类';

COMMENT ON COLUMN oa_vehicle.brand_model IS '品牌型号';

COMMENT ON COLUMN oa_vehicle.seat_count IS '座位数';

COMMENT ON COLUMN oa_vehicle.bare_price IS '裸车价格（元）';

COMMENT ON COLUMN oa_vehicle.compulsory_insurance_expire_time IS '交强险到期时间';

COMMENT ON COLUMN oa_vehicle.commercial_insurance_expire_time IS '商业险到期时间';

COMMENT ON COLUMN oa_vehicle.inspection_expire_time IS '年检到期时间';

COMMENT ON COLUMN oa_vehicle.pic_url IS '车辆照片 URL';

COMMENT ON COLUMN oa_vehicle.status IS '车辆台账状态';

COMMENT ON COLUMN oa_vehicle.sort IS '显示顺序';

COMMENT ON COLUMN oa_vehicle.remark IS '备注';

COMMENT ON COLUMN oa_vehicle.creator IS '创建者';

COMMENT ON COLUMN oa_vehicle.create_time IS '创建时间';

COMMENT ON COLUMN oa_vehicle.updater IS '更新者';

COMMENT ON COLUMN oa_vehicle.update_time IS '更新时间';

COMMENT ON COLUMN oa_vehicle.deleted IS '是否删除';

COMMENT ON TABLE oa_vehicle IS 'OA 车辆表';

COMMENT ON COLUMN oa_vehicle_apply.id IS '编号';

COMMENT ON COLUMN oa_vehicle_apply.no IS '用车申请单号';

COMMENT ON COLUMN oa_vehicle_apply.vehicle_id IS '车辆编号';

COMMENT ON COLUMN oa_vehicle_apply.vehicle_no IS '车牌号快照';

COMMENT ON COLUMN oa_vehicle_apply.user_id IS '申请人用户编号';

COMMENT ON COLUMN oa_vehicle_apply.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_vehicle_apply.start_time IS '预计出车时间';

COMMENT ON COLUMN oa_vehicle_apply.end_time IS '预计回车时间';

COMMENT ON COLUMN oa_vehicle_apply.start_location IS '出车地点';

COMMENT ON COLUMN oa_vehicle_apply.end_location IS '预计回车地点';

COMMENT ON COLUMN oa_vehicle_apply.reason IS '用车事由';

COMMENT ON COLUMN oa_vehicle_apply.passenger IS '随行人';

COMMENT ON COLUMN oa_vehicle_apply.status IS '审批状态';

COMMENT ON COLUMN oa_vehicle_apply.return_status IS '还车业务状态';

COMMENT ON COLUMN oa_vehicle_apply.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_vehicle_apply.remark IS '备注';

COMMENT ON COLUMN oa_vehicle_apply.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_vehicle_apply.creator IS '创建者';

COMMENT ON COLUMN oa_vehicle_apply.create_time IS '创建时间';

COMMENT ON COLUMN oa_vehicle_apply.updater IS '更新者';

COMMENT ON COLUMN oa_vehicle_apply.update_time IS '更新时间';

COMMENT ON COLUMN oa_vehicle_apply.deleted IS '是否删除';

COMMENT ON TABLE oa_vehicle_apply IS 'OA 用车申请表';

COMMENT ON COLUMN oa_vehicle_return.id IS '编号';

COMMENT ON COLUMN oa_vehicle_return.no IS '还车申请单号';

COMMENT ON COLUMN oa_vehicle_return.apply_id IS '用车申请编号';

COMMENT ON COLUMN oa_vehicle_return.vehicle_id IS '车辆编号，从用车申请生成';

COMMENT ON COLUMN oa_vehicle_return.user_id IS '申请人用户编号';

COMMENT ON COLUMN oa_vehicle_return.dept_id IS '申请部门编号';

COMMENT ON COLUMN oa_vehicle_return.actual_start_time IS '实际出车时间';

COMMENT ON COLUMN oa_vehicle_return.start_location IS '实际出车地点';

COMMENT ON COLUMN oa_vehicle_return.reason IS '用车事由';

COMMENT ON COLUMN oa_vehicle_return.passenger IS '随行人';

COMMENT ON COLUMN oa_vehicle_return.actual_return_time IS '实际回车时间';

COMMENT ON COLUMN oa_vehicle_return.return_location IS '实际回车地点';

COMMENT ON COLUMN oa_vehicle_return.status IS '审批状态';

COMMENT ON COLUMN oa_vehicle_return.process_instance_id IS 'BPM 流程实例编号';

COMMENT ON COLUMN oa_vehicle_return.remark IS '还车说明';

COMMENT ON COLUMN oa_vehicle_return.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_vehicle_return.creator IS '创建者';

COMMENT ON COLUMN oa_vehicle_return.create_time IS '创建时间';

COMMENT ON COLUMN oa_vehicle_return.updater IS '更新者';

COMMENT ON COLUMN oa_vehicle_return.update_time IS '更新时间';

COMMENT ON COLUMN oa_vehicle_return.deleted IS '是否删除';

COMMENT ON TABLE oa_vehicle_return IS 'OA 还车申请表';

COMMENT ON COLUMN oa_work_report.id IS '汇报编号';

COMMENT ON COLUMN oa_work_report.no IS '汇报单号';

COMMENT ON COLUMN oa_work_report.type IS '汇报类型';

COMMENT ON COLUMN oa_work_report.status IS '汇报状态';

COMMENT ON COLUMN oa_work_report.dept_id IS '汇报人所属部门编号';

COMMENT ON COLUMN oa_work_report.start_time IS '周期开始时间';

COMMENT ON COLUMN oa_work_report.end_time IS '周期结束时间';

COMMENT ON COLUMN oa_work_report.title IS '汇报标题';

COMMENT ON COLUMN oa_work_report.summary IS '工作总结';

COMMENT ON COLUMN oa_work_report.plan IS '工作计划补充说明';

COMMENT ON COLUMN oa_work_report.problem IS '问题与协调事项';

COMMENT ON COLUMN oa_work_report.work_items IS '已完成工作项';

COMMENT ON COLUMN oa_work_report.plan_items IS '工作计划项';

COMMENT ON COLUMN oa_work_report.file_urls IS '附件地址列表';

COMMENT ON COLUMN oa_work_report.remark IS '备注';

COMMENT ON COLUMN oa_work_report.creator IS '创建者';

COMMENT ON COLUMN oa_work_report.create_time IS '创建时间';

COMMENT ON COLUMN oa_work_report.updater IS '更新者';

COMMENT ON COLUMN oa_work_report.update_time IS '更新时间';

COMMENT ON COLUMN oa_work_report.deleted IS '是否删除';

COMMENT ON TABLE oa_work_report IS 'OA 工作汇报表';

COMMENT ON COLUMN oa_work_report_comment.id IS '点评编号';

COMMENT ON COLUMN oa_work_report_comment.report_id IS '工作汇报编号';

COMMENT ON COLUMN oa_work_report_comment.reviewer_id IS '点评人用户编号';

COMMENT ON COLUMN oa_work_report_comment.grade IS '评级：A/B/C';

COMMENT ON COLUMN oa_work_report_comment.review_text IS '点评正文';

COMMENT ON COLUMN oa_work_report_comment.reply IS '员工回复';

COMMENT ON COLUMN oa_work_report_comment.reply_time IS '回复时间';

COMMENT ON COLUMN oa_work_report_comment.creator IS '创建者';

COMMENT ON COLUMN oa_work_report_comment.create_time IS '创建时间';

COMMENT ON COLUMN oa_work_report_comment.updater IS '更新者';

COMMENT ON COLUMN oa_work_report_comment.update_time IS '更新时间';

COMMENT ON COLUMN oa_work_report_comment.deleted IS '是否删除';

COMMENT ON TABLE oa_work_report_comment IS '工作汇报点评与回复';

COMMENT ON COLUMN portal_categories.scope IS '业务归属，点号分层：material.sample / material.office / training.course';

COMMENT ON COLUMN portal_categories.name IS '展示名；物资侧另由注册表限 32（请求体冻结面）';

COMMENT ON COLUMN portal_categories.code IS '程序锚点（路由 slug / seed 幂等键），普通分类留空';

COMMENT ON COLUMN portal_categories.depth IS '1 = 顶层（物资品类、培训大类），2 = 子级（培训小类）';

COMMENT ON COLUMN portal_categories.sort_order IS '同层展示序，再按 name';

COMMENT ON COLUMN portal_categories.attrs IS '各业务自己的开关与展示字段，键名按 scope 白名单校验';

COMMENT ON COLUMN portal_categories.archived_at IS 'NULL = 在役；时间戳本身即审计信息，不用 boolean';

COMMENT ON COLUMN portal_categories.created_by IS '创建人 username；迁移 seed 行写 migration';

COMMENT ON COLUMN portal_categories.active_name IS '在役行才非 NULL，给下面的唯一索引当锚：等价于 PG 的部分唯一索引';

COMMENT ON TABLE portal_categories IS '全门户共用分类字典：scope + depth + attrs + 归档';

COMMENT ON COLUMN portal_news.content IS '富文本正文，使用 TEXT 保留完整长文';

COMMENT ON COLUMN portal_news.category IS '文本分类，与 portal_categories 无外键：动态的分类口径独立于物资/培训';

COMMENT ON COLUMN portal_news.template_type IS 'simple / 模板化稿型，模板特有字段在 blocks';

COMMENT ON COLUMN portal_news.blocks IS '模板数据块（含 dept 等前端要读的字段）';

COMMENT ON COLUMN portal_news.author IS '作者展示名快照';

COMMENT ON COLUMN portal_news.author_username IS '作者 username（= 企微 userid）';

COMMENT ON COLUMN portal_news.is_featured IS '精选，前端兼作置顶展示';

COMMENT ON COLUMN portal_news.pending_notify_wecom IS '作者提交的企微推送意图，审批通过时由审核人二次确认并一次性消费';

COMMENT ON COLUMN portal_news.pending_notify_dept_scope IS '推送部门范围，NULL = 全员';

COMMENT ON TABLE portal_news IS '公司动态：审批状态机 + 模板 blocks + 企微推送意图';

COMMENT ON COLUMN portal_news_comment.content IS '软删除后写占位文案 [该评论已删除]，列复用不另开 flag 文案';

COMMENT ON TABLE portal_news_comment IS '动态评论（含楼中楼），删除是软删';

COMMENT ON TABLE portal_news_like IS '动态点赞：每人每动态最多一次';

COMMENT ON COLUMN portal_page_views.username IS 'NULL = 匿名访问';

COMMENT ON COLUMN portal_page_views.display_name IS '打点时的名字快照';

COMMENT ON COLUMN portal_page_views.department IS '打点时的主部门名快照';

COMMENT ON TABLE portal_page_views IS '页面浏览记录：谁看了什么、哪些页面热门';

COMMENT ON COLUMN portal_wiki.department IS '单值归属部门（可见范围口径），不是 portal_categories 的行';

COMMENT ON COLUMN portal_wiki.doc_url IS '外链文档地址，正文可为空只给链接';

COMMENT ON COLUMN portal_wiki.tags IS '标签数组（原 PG ARRAY(String)），接口层进出都是数组';

COMMENT ON TABLE portal_wiki IS '知识库文档';

COMMENT ON COLUMN recognitions.type IS 'like/praise/star_nomination';

COMMENT ON TABLE recognitions IS '收到的认可';

COMMENT ON COLUMN rhythm_node_follow.id IS '编号';

COMMENT ON COLUMN rhythm_node_follow.user_id IS '员工用户编号';

COMMENT ON COLUMN rhythm_node_follow.node_id IS '节奏节点编号';

COMMENT ON COLUMN rhythm_node_follow.remind_before IS '开始前几天提醒';

COMMENT ON COLUMN rhythm_node_follow.created_at IS '创建时间';

COMMENT ON TABLE rhythm_node_follow IS '我关注的节奏节点';

COMMENT ON COLUMN rhythm_nodes.name IS '节点名称';

COMMENT ON COLUMN rhythm_nodes.kind IS 'sale / new / event';

COMMENT ON COLUMN rhythm_nodes.dept_actions IS '各部门作战安排，JSON';

COMMENT ON COLUMN rhythm_nodes.note IS '作战说明';

COMMENT ON COLUMN rhythm_nodes.linked_refs IS '关联任务 / 文档，逗号分隔';

COMMENT ON TABLE rhythm_nodes IS '大促·新品节奏节点';

COMMENT ON COLUMN sys_dept.id IS '企微部门 ID，直接沿用不再生成';

COMMENT ON COLUMN sys_dept.path IS '企微网关解析好的全路径';

COMMENT ON COLUMN sys_dept.leader_userid IS '部门负责人企微 userid；网关暂未下发，可手工维护';

COMMENT ON COLUMN sys_dept.leader_userids IS '全部负责人企微 userid 数组；leader_userid 存首个';

COMMENT ON COLUMN sys_dept.active IS '通讯录里已消失的部门置 0，不物理删除以保留历史归属';

COMMENT ON TABLE sys_dept IS '部门：来自企微通讯录';

COMMENT ON COLUMN sys_dict_type.type IS '字典类型，移植时直接沿用芋道的 hrm_* / oa_* 命名';

COMMENT ON COLUMN sys_directory_sync_log.trigger_type IS 'manual / schedule';

COMMENT ON COLUMN sys_directory_sync_log.actor IS '手动触发时的人，定时为 system';

COMMENT ON COLUMN sys_directory_sync_log.ok IS 'NULL=杩愯?涓?紝1=鎴愬姛锛?=澶辫触';

COMMENT ON COLUMN sys_directory_sync_log.duration_ms IS '杩愯?涓?繕娌℃湁鑰楁椂锛屼笉鑳芥嬁 0 鍐掑厖';

COMMENT ON TABLE sys_directory_sync_log IS '通讯录同步运行记录：运维按时间倒序看每次结果';

COMMENT ON COLUMN sys_employee_sync_job.actor_user_id IS '发起人，对应 sys_user.id（原表存的是 username）';

COMMENT ON COLUMN sys_employee_sync_job.status IS 'queued/running/ready/applying/applied/failed/stale';

COMMENT ON COLUMN sys_employee_sync_job.stage IS '当前阶段，用于前端进度条';

COMMENT ON COLUMN sys_employee_sync_job.worker_token IS '租约持有者令牌，防两个 worker 抢同一任务';

COMMENT ON COLUMN sys_employee_sync_job.lease_expires_at IS '租约到期时间，过期可被接管';

COMMENT ON COLUMN sys_employee_sync_job.scan_summary IS '扫描结果汇总';

COMMENT ON COLUMN sys_employee_sync_job.preview IS '待确认差异预览';

COMMENT ON COLUMN sys_employee_sync_job.unmatched_candidates IS '企微里找不到对应员工的差异';

COMMENT ON COLUMN sys_employee_sync_job.apply_summary IS '落库结果汇总';

COMMENT ON COLUMN sys_employee_sync_job.expires_at IS '任务本身的过期时间（与租约不同）';

COMMENT ON TABLE sys_employee_sync_job IS '企微员工资料扫描任务：扫描只出差异，确认后才写档案';

COMMENT ON COLUMN sys_menu.type IS 'dir / menu / button';

COMMENT ON COLUMN sys_menu.path IS '前端路由，button 类型为空';

COMMENT ON COLUMN sys_menu.permission IS '权限串，沿用芋道 oa:leave:create 风格，便于移植代码直接对接';

COMMENT ON TABLE sys_menu IS '菜单与按钮权限';

COMMENT ON COLUMN sys_operate_log.trace_id IS '链路追踪编号';

COMMENT ON COLUMN sys_operate_log.user_id IS '操作人，对应 sys_user.id';

COMMENT ON COLUMN sys_operate_log.user_type IS '用户类型，与 Sa-Token 体系对齐';

COMMENT ON COLUMN sys_operate_log.type IS '操作模块类型，如 公司动态/HR 文档';

COMMENT ON COLUMN sys_operate_log.sub_type IS '操作名，如 发布/归档';

COMMENT ON COLUMN sys_operate_log.biz_id IS '被操作对象的业务编号';

COMMENT ON COLUMN sys_operate_log.action IS '操作内容摘要';

COMMENT ON COLUMN sys_operate_log.extra IS '拓展字段';

COMMENT ON COLUMN sys_operate_log.target_table IS 'Portal 口径：被改动的表名';

COMMENT ON COLUMN sys_operate_log.before_value IS 'Portal 口径：改动前快照';

COMMENT ON COLUMN sys_operate_log.after_value IS 'Portal 口径：改动后快照';

COMMENT ON COLUMN sys_operate_log.comment IS '操作说明';

COMMENT ON TABLE sys_operate_log IS '操作日志：谁在什么时候把哪条记录改成了什么样';

COMMENT ON COLUMN sys_role.code IS '角色标识，如 admin / hr / ops / manager / employee';

COMMENT ON COLUMN sys_role.built_in IS '内置角色不允许删除';

COMMENT ON TABLE sys_role IS '角色：权限在本地维护，成员来源仍是企微';

COMMENT ON COLUMN sys_user.wecom_userid IS '企微成员 userid；纯本地账号为 NULL';

COMMENT ON COLUMN sys_user.username IS '登录名，与企微 userid 保持一致';

COMMENT ON COLUMN sys_user.name IS '姓名，以企微通讯录为准';

COMMENT ON COLUMN sys_user.position IS '岗位，来自企微';

COMMENT ON COLUMN sys_user.main_dept_id IS '主部门，对应 sys_dept.id';

COMMENT ON COLUMN sys_user.wecom_status IS '企微原始状态：1 已激活 2 已禁用 4 未激活 5 已离职';

COMMENT ON COLUMN sys_user.enabled IS '是否允许登录；企微非激活成员同步后置 0';

COMMENT ON COLUMN sys_user.password_hash IS '仅本地口令登录使用，线上走企微不依赖该字段';

COMMENT ON COLUMN sys_user.directory_synced_at IS '最近一次由通讯录同步覆盖的时间';

COMMENT ON TABLE sys_user IS '登录账号：由企微通讯录同步生成';

COMMENT ON TABLE sys_user_dept IS '成员与部门的多对多（企微允许一人多部门）';

COMMENT ON COLUMN voices.user_id IS '为空表示匿名';

COMMENT ON COLUMN voices.channel IS 'suggestion/ceo/anonymous/survey';

COMMENT ON COLUMN voices.anonymous IS '1 对读者隐去作者姓名';

COMMENT ON TABLE voices IS '我要发声';

COMMENT ON TABLE portal_entry_rollout IS '门户管理入口上线设置';

COMMENT ON COLUMN portal_esports_settings.open_weekdays IS '开放星期（ISO 1-7），原 PG ARRAY(int)';

COMMENT ON COLUMN portal_esports_settings.approver_user_id IS '第一环节第一人投影 sys_user.id；新口径事实源是 approval_stages';

COMMENT ON COLUMN portal_esports_settings.approval_stages IS '有序审批环节数组 [{approver_user_ids:[...]}]，原 PG JSONB';

COMMENT ON TABLE portal_esports_settings IS '电竞室开放规则（单行 id=1，多级审批由 approval_stages 驱动）';

COMMENT ON COLUMN portal_esports_bookings.participants IS '参加人快照 [{user_id,display_name}]，原 PG JSONB；NULL=旧单无名单';

COMMENT ON COLUMN portal_esports_bookings.applicant_user_id IS '申请人 sys_user.id';

COMMENT ON COLUMN portal_esports_bookings.approver_user_id IS '第一环节第一人投影 sys_user.id';

COMMENT ON COLUMN portal_esports_bookings.current_stage_index IS '0 起始；NULL=流程已结束';

COMMENT ON TABLE portal_esports_bookings IS '电竞室独占预约单（pending/approved 均占位）';

COMMENT ON COLUMN portal_esports_approval_steps.approver_user_id IS '指定审批人 sys_user.id';

COMMENT ON COLUMN portal_esports_approval_steps.acted_by_user_id IS '实际代审人 sys_user.id（同环节他人可代审）';

COMMENT ON TABLE portal_esports_approval_steps IS '电竞预约每单每环节审批人快照';

COMMENT ON COLUMN portal_esports_booking_logs.actor_user_id IS '操作人 sys_user.id';

COMMENT ON TABLE portal_esports_booking_logs IS '电竞预约提交/审批/取消流水';

COMMENT ON COLUMN portal_events.title IS '活动标题';

COMMENT ON COLUMN portal_events.description IS '活动介绍';

COMMENT ON COLUMN portal_events.location IS '举办地点';

COMMENT ON COLUMN portal_events.start_date IS '开始日期（新建缺省今天）';

COMMENT ON COLUMN portal_events.end_date IS '结束日期，可空';

COMMENT ON COLUMN portal_events.event_type IS '活动类型';

COMMENT ON COLUMN portal_events.status IS '状态：只有 published';

COMMENT ON TABLE portal_events IS '展会/活动日历';

COMMENT ON COLUMN portal_products.name IS '产品名称';

COMMENT ON COLUMN portal_products.brand IS '品牌';

COMMENT ON COLUMN portal_products.category IS '分类';

COMMENT ON COLUMN portal_products.description IS '介绍';

COMMENT ON COLUMN portal_products.image_url IS '图片地址';

COMMENT ON COLUMN portal_products.link_url IS '详情链接';

COMMENT ON COLUMN portal_products.launch_date IS '发布日期，可空';

COMMENT ON COLUMN portal_products.status IS '状态：只有 published';

COMMENT ON COLUMN portal_products.is_featured IS '精选，前端兼作置顶展示';

COMMENT ON TABLE portal_products IS '新品动态';

COMMENT ON COLUMN portal_feedback.username IS '提交人（企微 userid = Portal 的 wecom_userid）';

COMMENT ON COLUMN portal_feedback.reporter_name IS '提交人姓名快照';

COMMENT ON COLUMN portal_feedback.department IS '主部门名快照（单值）';

COMMENT ON COLUMN portal_feedback.category IS '反馈分类（产品/内部/建议等，自由文本）';

COMMENT ON COLUMN portal_feedback.sentiment IS 'AI 情感（bot 写入，迁移时保留占位）';

COMMENT ON COLUMN portal_feedback.raw_message IS '员工原文（description）';

COMMENT ON COLUMN portal_feedback.image_urls IS '图片地址数组；原 PG JSONB';

COMMENT ON COLUMN portal_feedback.ai_summary IS 'AI 摘要（bot 写入）';

COMMENT ON COLUMN portal_feedback.assigned_to IS '指派处理人（bot/管理写入）';

COMMENT ON COLUMN portal_feedback.resolved_note IS '解决说明';

COMMENT ON TABLE portal_feedback IS '员工反馈：员工提交 + 管理查看；AI 富化随 bot 落地';

COMMENT ON TABLE portal_hr_layout_jobs IS 'HR 编辑器智能排版任务（按用户隔离）';

COMMENT ON TABLE portal_hr_layout_attempts IS '排版请求限额（包含重试，每人每小时20次）';

COMMENT ON COLUMN portal_hr_documents.slug IS '旧 URL 兼容身份；新文档不分配。删除不释放 slug';

COMMENT ON COLUMN portal_hr_documents.category IS 'handbook|incentive|hr-sop|hr|policy';

COMMENT ON COLUMN portal_hr_documents.topic IS '二级分类（制度中心 6 主题）；NULL=存量回落 admin';

COMMENT ON COLUMN portal_hr_documents.content_html IS '已发布快照（HTML 格式）；块格式文档此列为空串';

COMMENT ON COLUMN portal_hr_documents.draft_content IS '在编草稿（HTML 格式）；块格式草稿为 NULL';

COMMENT ON COLUMN portal_hr_documents.content_blocks IS '已发布快照（块格式 JSON 数组）；NULL=老 HTML 格式';

COMMENT ON COLUMN portal_hr_documents.draft_blocks IS '在编草稿（块格式）；与 draft_content 二选一非空';

COMMENT ON COLUMN portal_hr_documents.draft_version IS '草稿目标版本号；发布后清空';

COMMENT ON COLUMN portal_hr_documents.dept_scope IS '部门访问限制（部门名数组）；NULL=全员；原 PG ARRAY(String)';

COMMENT ON COLUMN portal_hr_documents.intro IS '引言（纯文本 ≤500 字）';

COMMENT ON COLUMN portal_hr_documents.cover_image IS '封面图 https:// 或 / 开头；空串=主题渐变';

COMMENT ON COLUMN portal_hr_documents.theme IS ''''' | 6 预设 | #RRGGBB';

COMMENT ON COLUMN portal_hr_documents.tags IS '标签数组；原 PG ARRAY(String)';

COMMENT ON COLUMN portal_hr_documents.cover IS '整屏封面配置（bg_type/image/label_*/icon/desc/date/scope/dept/nav_tabs）';

COMMENT ON COLUMN portal_hr_documents.signoff IS '签收卡配置（icon/title/lead/ack_label/button_text/tip/signed_note）';

COMMENT ON COLUMN portal_hr_documents.published_at IS '判「已发布」用此列而非 status（改稿会切回 draft 但保留它）';

COMMENT ON COLUMN portal_hr_documents.offline_at IS '下架标记：员工端可见性 = published_at 非空 且 offline_at 空';

COMMENT ON COLUMN portal_hr_documents.deleted_at IS '逻辑删除：进回收站，管理列表默认隐藏';

COMMENT ON TABLE portal_hr_documents IS 'HR 文档 CMS 主表：草稿/发布双列 + 下架/逻辑删除 + 块/HTML 双格式';

COMMENT ON COLUMN portal_hr_document_aliases.alias_type IS 'slug|legacy_key|legacy_path';

COMMENT ON TABLE portal_hr_document_aliases IS 'HR 文档旧地址别名（历史 URL 兼容解析）';

COMMENT ON TABLE portal_hr_document_entry_bindings IS 'HR 文档固定入口绑定（业务入口 → 文档 ID）';

COMMENT ON COLUMN portal_hr_document_versions.slug IS '发布时 slug 快照；新文档可为 NULL';

COMMENT ON COLUMN portal_hr_document_versions.content_html IS '块格式快照此列存空串（判别靠 content_blocks）';

COMMENT ON COLUMN portal_hr_document_versions.content_blocks IS '块格式快照；NULL=老 HTML 版本';

COMMENT ON TABLE portal_hr_document_versions IS 'HR 文档发布版本快照（仲裁举证，只增不可删）';

COMMENT ON COLUMN portal_hr_document_draft_versions.content_html IS 'HTML 快照；块格式快照此列为 NULL';

COMMENT ON COLUMN portal_hr_document_draft_versions.content_blocks IS '块格式快照（判别同主表）';

COMMENT ON TABLE portal_hr_document_draft_versions IS 'HR 文档草稿版本快照（工作历史，可删）';

COMMENT ON COLUMN portal_handbook_signoff.user_id IS '签收人 sys_user.id（Portal 的 portal_users.id 原样保留）';

COMMENT ON COLUMN portal_handbook_signoff.username IS '签收人企微 userid 快照';

COMMENT ON COLUMN portal_handbook_signoff.document_id IS '稳定文档 ID → portal_hr_documents.id（RESTRICT）';

COMMENT ON COLUMN portal_handbook_signoff.document IS '文档标识快照（slug；老记录可能是 legacy alias）';

COMMENT ON TABLE portal_handbook_signoff IS '员工手册/制度文档线上签收记录（人事档案存档）';

COMMENT ON COLUMN portal_training.description IS '纯文本简介（不是富文本）';

COMMENT ON COLUMN portal_training.department IS '老列：早期按单一部门字符串归属，新口径用 dept_scope，此列不再写';

COMMENT ON COLUMN portal_training.doc_url IS '老列：老课程资料外链，重做后已各迁成一条 link 课时，不再写';

COMMENT ON COLUMN portal_training.video_url IS '老列：同上';

COMMENT ON COLUMN portal_training.category IS '小类名的过渡期镜像；事实源是 category_id';

COMMENT ON COLUMN portal_training.category_id IS '小类 → portal_categories(scope=training.course, depth=2)；NULL = 员工端「未分类」兜底';

COMMENT ON COLUMN portal_training.source_prefix IS '导入幂等键（bucket 课程目录前缀），手建课程为 NULL';

COMMENT ON COLUMN portal_training.dept_scope IS '可见部门名单（部门名），NULL = 全员；原 PG ARRAY(String)';

COMMENT ON COLUMN portal_training.author_username IS '手建课程的创建人；导入课程为 NULL';

COMMENT ON TABLE portal_training IS '培训课程：draft|published + 小类归属 + 部门可见范围';

COMMENT ON COLUMN portal_training_lessons.pages IS '图片键数组（cards / 图片序列型 article）；原 PG JSONB';

COMMENT ON COLUMN portal_training_lessons.url IS '外链地址（仅 link 课时）';

COMMENT ON COLUMN portal_training_lessons.duration_sec IS '视频时长（秒），播放器首次心跳回报后写入';

COMMENT ON COLUMN portal_training_lessons.source_prefix IS '导入幂等键（课时目录/文件键），手建课时为 NULL';

COMMENT ON TABLE portal_training_lessons IS '培训课时：四种形态 + link，对象键不存 URL';

COMMENT ON COLUMN portal_training_lesson_progress.position IS '断点：视频秒 / PDF 页 / 卡片索引';

COMMENT ON COLUMN portal_training_lesson_progress.duration_sec IS '播放器回报的媒体总时长';

COMMENT ON TABLE portal_training_lesson_progress IS '员工×课时的学习进度：只增不减的 percent + 完成阈值';

COMMENT ON COLUMN portal_training_progress.training_id IS '无 FK：Portal 语义如此，课程删除后进度留存';

COMMENT ON TABLE portal_training_progress IS '员工×课程级进度汇总（legacy，无唯一约束）';

COMMENT ON COLUMN hrm_contract_template.kind IS 'onboard / offboard';

COMMENT ON TABLE hrm_wecom_roster_sync_run IS '企微通讯录与人事花名册同步记录';

COMMENT ON COLUMN oa_wecom_approval_link.id IS '主键';

COMMENT ON COLUMN oa_wecom_approval_link.apply_type IS '业务类型：leave/overtime/reimbursement/travel/travel-reimbursement/seal/supply/hrm-leave';

COMMENT ON COLUMN oa_wecom_approval_link.biz_id IS '业务单据编号';

COMMENT ON COLUMN oa_wecom_approval_link.request_id IS '提交幂等请求号 oa-{type}-{bizId}';

COMMENT ON COLUMN oa_wecom_approval_link.approval_no IS '企微审批单号，出单后回填';

COMMENT ON COLUMN oa_wecom_approval_link.creator IS '创建者';

COMMENT ON COLUMN oa_wecom_approval_link.updater IS '更新者';

COMMENT ON COLUMN oa_wecom_approval_link.create_time IS '创建时间';

COMMENT ON COLUMN oa_wecom_approval_link.update_time IS '更新时间';

COMMENT ON COLUMN oa_wecom_approval_link.deleted IS '是否删除';

COMMENT ON TABLE oa_wecom_approval_link IS '业务单据与企微审批单链接表';

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_attendance_clock FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_attendance_group FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_attendance_holiday FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_attendance_leave FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_config FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time','active_user_id');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_certificate FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_change_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_contact FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_contract FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_education_experience FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_file FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_personal_note FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_quit_info FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_salary_card FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_training_experience FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_employee_work_experience FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_insurance_employee_info FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_insurance_month_employee_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_insurance_month_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_insurance_scheme FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_insurance_scheme_project FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_action_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_appeal_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_dimension FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_quota FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_quota_score FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_stage FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_assessment_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_plan FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_performance_result_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_recruit_candidate FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_recruit_channel FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_recruit_interview FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_recruit_post FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_recruit_post_type FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_change_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_change_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_config FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_employee_info FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_group FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_month_employee_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_month_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_option FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_option_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_slip FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_slip_send_record FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_slip_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON hrm_salary_tax_rule FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_announcement FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_announcement_receiver FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_attendance FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_leave_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_meeting_room FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_meeting_room_booking FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_official_doc_receive FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_official_doc_send FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_official_doc_template FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_overtime_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_regular_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_reimbursement FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_resign_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_schedule FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_schedule_participant FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_seal FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_seal_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_supply_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_supply_apply_item FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_supply_item FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_task FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_task_log FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_task_receiver FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_travel_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_travel_reimbursement FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_vehicle FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_vehicle_apply FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_vehicle_return FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_work_report FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_work_report_comment FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_categories FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at','active_name');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_news FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_news_comment FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_wiki FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON sys_dept FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON sys_employee_sync_job FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON sys_user FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_esports_settings FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_esports_bookings FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_events FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_products FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_feedback FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_hr_document_aliases FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_hr_document_entry_bindings FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_brand_docs FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_contract_entities FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_celebration_settings FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_resume_interview_processes FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at','active_resume_id');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_resume_interview_rounds FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_resumes FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_training FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_training_lessons FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_training_lesson_progress FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_updated_at BEFORE UPDATE ON portal_workspace_records FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('updated_at');

CREATE TRIGGER cyt_touch_update_time BEFORE UPDATE ON oa_wecom_approval_link FOR EACH ROW EXECUTE FUNCTION cyt_touch_updated_column('update_time');
