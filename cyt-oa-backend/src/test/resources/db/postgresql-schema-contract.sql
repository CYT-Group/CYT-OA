BEGIN;
DO $$
DECLARE
  user_id bigint;
  employee_id bigint;
  actual timestamp;
BEGIN
  INSERT INTO sys_user(username,name,updated_at) VALUES('schema_check_user','initial','2001-01-01') RETURNING id INTO user_id;
  IF user_id <= 1 THEN RAISE EXCEPTION 'identity did not advance after explicit seed'; END IF;
  UPDATE sys_user SET name=name WHERE id=user_id RETURNING updated_at INTO actual;
  IF actual <> TIMESTAMP '2001-01-01' THEN RAISE EXCEPTION 'no-op update changed timestamp'; END IF;
  UPDATE sys_user SET name='explicit',updated_at='2002-01-01' WHERE id=user_id RETURNING updated_at INTO actual;
  IF actual <> TIMESTAMP '2002-01-01' THEN RAISE EXCEPTION 'explicit timestamp overwritten'; END IF;
  UPDATE sys_user SET name='automatic' WHERE id=user_id RETURNING updated_at INTO actual;
  IF actual <= TIMESTAMP '2002-01-01' THEN RAISE EXCEPTION 'changed row timestamp not updated'; END IF;
  -- BEFORE triggers see generated columns as NULL. They must not cause false changes.
  INSERT INTO hrm_employee(user_id,name,create_time,update_time)
    VALUES(7896543212345,'schema_check_employee','2001-01-01','2001-01-01') RETURNING id INTO employee_id;
  UPDATE hrm_employee SET name=name WHERE id=employee_id RETURNING update_time INTO actual;
  IF actual <> TIMESTAMP '2001-01-01' THEN RAISE EXCEPTION 'generated column caused no-op timestamp update'; END IF;
  BEGIN
    INSERT INTO sys_user(username,name) VALUES('ADMIN','duplicate');
    RAISE EXCEPTION 'case-insensitive username uniqueness lost';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
  INSERT INTO sys_user(username,name,wecom_userid) VALUES('schema_check_wecom','case','MixedUserId');
  BEGIN
    INSERT INTO sys_user(username,name,wecom_userid) VALUES('schema_check_wecom2','case','mixeduserid');
    RAISE EXCEPTION 'case-insensitive wecom userid uniqueness lost';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
  BEGIN
    INSERT INTO portal_entry_rollout(id,entries,revision) VALUES(2,'invalid json',0);
    RAISE EXCEPTION 'invalid JSON accepted';
  EXCEPTION WHEN invalid_text_representation THEN NULL;
  END;
  IF cyt_json_fingerprint('{"b":2,"a":1}') <> cyt_json_fingerprint('{"a":1,"b":2}') THEN
    RAISE EXCEPTION 'variant fingerprint is not canonical';
  END IF;
  INSERT INTO portal_categories(scope,name,code,created_by) VALUES('schema.test','CategoryName','CodeName','test');
  BEGIN
    INSERT INTO portal_categories(scope,name,created_by) VALUES('schema.test','categoryname','test');
    RAISE EXCEPTION 'active category case-insensitive uniqueness lost';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
  BEGIN
    INSERT INTO portal_categories(scope,name,code,created_by) VALUES('schema.test','other category','codename','test');
    RAISE EXCEPTION 'category code case-insensitive uniqueness lost';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
END
$$;
ROLLBACK;
