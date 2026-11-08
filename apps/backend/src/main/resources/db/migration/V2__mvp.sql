CREATE TABLE rideguard.users (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE CHECK(email=lower(btrim(email)) AND length(email)>=3),
 full_name varchar(100) NOT NULL CHECK(length(btrim(full_name)) BETWEEN 1 AND 100), password_hash text NOT NULL,
 role varchar(10) NOT NULL DEFAULT 'USER' CHECK(role IN ('USER','ADMIN')),
 status varchar(10) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','LOCKED')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL CHECK(updated_at>=created_at)
);
CREATE INDEX users_list ON rideguard.users(status,created_at DESC,id DESC);
CREATE TABLE rideguard.auth_sessions (
 id uuid PRIMARY KEY, family_id uuid NOT NULL REFERENCES rideguard.auth_sessions(id) DEFERRABLE INITIALLY DEFERRED,
 user_id uuid NOT NULL REFERENCES rideguard.users(id), generation integer NOT NULL CHECK(generation>=0),
 refresh_token_hash text NOT NULL UNIQUE, client_type varchar(16) NOT NULL CHECK(client_type IN ('mobile','admin_web')),
 client_name varchar(100), created_at timestamptz NOT NULL, last_seen_at timestamptz NOT NULL,
 expires_at timestamptz NOT NULL, consumed_at timestamptz, revoked_at timestamptz,
 replaced_by_session_id uuid REFERENCES rideguard.auth_sessions(id) DEFERRABLE INITIALLY DEFERRED,
 UNIQUE(family_id,generation), CHECK(expires_at>created_at AND expires_at<=created_at+interval '30 days'),
 CHECK(last_seen_at>=created_at), CHECK(consumed_at>=created_at), CHECK(revoked_at>=created_at),
 CHECK(replaced_by_session_id<>id), CHECK((generation=0)=(family_id=id))
);
CREATE UNIQUE INDEX session_live_leaf ON rideguard.auth_sessions(family_id) WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX sessions_user ON rideguard.auth_sessions(user_id,created_at DESC,id DESC);
CREATE INDEX sessions_expiry ON rideguard.auth_sessions(expires_at);
CREATE TABLE rideguard.password_reset_tokens (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES rideguard.users(id),token_hash text NOT NULL UNIQUE,
 created_at timestamptz NOT NULL,expires_at timestamptz NOT NULL,used_at timestamptz,
 CHECK(expires_at>created_at AND expires_at<=created_at+interval '15 minutes'),CHECK(used_at>=created_at)
);
CREATE INDEX resets_user ON rideguard.password_reset_tokens(user_id,expires_at);
CREATE INDEX resets_expiry ON rideguard.password_reset_tokens(expires_at);
CREATE TABLE rideguard.devices (
 id uuid PRIMARY KEY,serial_number varchar(128) NOT NULL UNIQUE CHECK(serial_number ~ '^[A-Z0-9][A-Z0-9-]{0,127}$'),
 model varchar(100) NOT NULL CHECK(length(btrim(model))>0),hardware_revision varchar(64) CHECK(length(btrim(hardware_revision))>0),
 status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DISABLED','RETIRED')),
 created_at timestamptz NOT NULL,updated_at timestamptz NOT NULL CHECK(updated_at>=created_at)
);
CREATE INDEX devices_list ON rideguard.devices(status,created_at DESC,id DESC);
CREATE TABLE rideguard.device_bindings (
 id uuid PRIMARY KEY,device_id uuid NOT NULL REFERENCES rideguard.devices(id),user_id uuid NOT NULL REFERENCES rideguard.users(id),
 nickname varchar(60) CHECK(length(btrim(nickname))>0),bound_at timestamptz NOT NULL,unbound_at timestamptz,
 CHECK(unbound_at>=bound_at),UNIQUE(id,device_id,user_id)
);
CREATE UNIQUE INDEX device_active_binding ON rideguard.device_bindings(device_id) WHERE unbound_at IS NULL;
CREATE INDEX bindings_user ON rideguard.device_bindings(user_id,bound_at DESC,id DESC);
CREATE INDEX bindings_device ON rideguard.device_bindings(device_id,bound_at DESC,id DESC);
CREATE FUNCTION rideguard.valid_settings(s jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
 SELECT jsonb_typeof(s)='object' AND s ?& ARRAY['display_brightness','vibration_enabled']
 AND s - ARRAY['display_brightness','vibration_enabled']='{}'::jsonb
 AND jsonb_typeof(s->'display_brightness')='number'
 AND (s->>'display_brightness') ~ '^(0|[1-9][0-9]?|100)$'
 AND jsonb_typeof(s->'vibration_enabled')='boolean'
$$;
CREATE TABLE rideguard.device_config_versions (
 binding_id uuid NOT NULL REFERENCES rideguard.device_bindings(id),version bigint NOT NULL CHECK(version BETWEEN 1 AND 9007199254740991),
 schema_version integer NOT NULL CHECK(schema_version=1),settings jsonb NOT NULL CHECK(rideguard.valid_settings(settings)),
 issued_at timestamptz NOT NULL,PRIMARY KEY(binding_id,version)
);
CREATE TABLE rideguard.device_configs (
 binding_id uuid PRIMARY KEY REFERENCES rideguard.device_bindings(id),schema_version integer NOT NULL CHECK(schema_version=1),
 desired_version bigint NOT NULL CHECK(desired_version BETWEEN 1 AND 9007199254740991),
 reported_applied_version bigint CHECK(reported_applied_version>=1 AND reported_applied_version<=desired_version),
 settings jsonb NOT NULL CHECK(rideguard.valid_settings(settings)),updated_at timestamptz NOT NULL,reported_at timestamptz,
 CHECK((reported_at IS NULL)=(reported_applied_version IS NULL)),
 FOREIGN KEY(binding_id,desired_version) REFERENCES rideguard.device_config_versions(binding_id,version) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(binding_id,reported_applied_version) REFERENCES rideguard.device_config_versions(binding_id,version) DEFERRABLE INITIALLY DEFERRED
);
CREATE FUNCTION rideguard.check_config_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM rideguard.device_config_versions v WHERE v.binding_id=NEW.binding_id AND v.version=NEW.desired_version
 AND v.schema_version=NEW.schema_version AND v.settings=NEW.settings) THEN RAISE EXCEPTION 'invalid config snapshot' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER config_snapshot AFTER INSERT OR UPDATE ON rideguard.device_configs DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION rideguard.check_config_snapshot();
CREATE FUNCTION rideguard.immutable_version() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'immutable version' USING ERRCODE='23514'; END $$;
CREATE TRIGGER immutable_version BEFORE UPDATE ON rideguard.device_config_versions FOR EACH ROW EXECUTE FUNCTION rideguard.immutable_version();
CREATE TABLE rideguard.feedbacks (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES rideguard.users(id),device_id uuid REFERENCES rideguard.devices(id),binding_id uuid,
 category varchar(16) NOT NULL CHECK(category IN ('BUG','SUGGESTION','OTHER')),content text NOT NULL CHECK(length(btrim(content)) BETWEEN 1 AND 5000),
 status varchar(16) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','IN_PROGRESS','RESOLVED')),
 admin_response text CHECK(length(btrim(admin_response)) BETWEEN 1 AND 5000),responded_at timestamptz,responded_by uuid REFERENCES rideguard.users(id),
 created_at timestamptz NOT NULL,updated_at timestamptz NOT NULL CHECK(updated_at>=created_at),
 CHECK((device_id IS NULL)=(binding_id IS NULL)),CHECK((admin_response IS NULL)=(responded_at IS NULL)),
 CHECK((admin_response IS NULL)=(responded_by IS NULL)),CHECK(responded_at>=created_at),
 FOREIGN KEY(binding_id,device_id,user_id) REFERENCES rideguard.device_bindings(id,device_id,user_id)
);
CREATE INDEX feedback_user ON rideguard.feedbacks(user_id,created_at DESC,id DESC);
CREATE INDEX feedback_status ON rideguard.feedbacks(status,created_at DESC,id DESC);
CREATE INDEX feedback_device ON rideguard.feedbacks(device_id);
CREATE INDEX feedback_binding ON rideguard.feedbacks(binding_id);
CREATE INDEX feedback_responder ON rideguard.feedbacks(responded_by);
CREATE TABLE rideguard.idempotency_records (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES rideguard.users(id),method varchar(8) NOT NULL CHECK(method='POST'),
 route varchar(128) NOT NULL CHECK(route IN ('/api/v1/devices/bind','/api/v1/feedbacks')),idempotency_key uuid NOT NULL,request_hash text NOT NULL,
 resource_type varchar(16) NOT NULL CHECK(resource_type IN ('BINDING','FEEDBACK')),
 binding_id uuid REFERENCES rideguard.device_bindings(id),feedback_id uuid REFERENCES rideguard.feedbacks(id),
 response_status smallint CHECK(response_status IN (200,201)),response_body jsonb,
 created_at timestamptz NOT NULL,completed_at timestamptz NOT NULL,response_expires_at timestamptz NOT NULL,
 state varchar(16) NOT NULL CHECK(state IN ('COMPLETED','EXPIRED')),UNIQUE(user_id,method,route,idempotency_key),
 CHECK(completed_at>=created_at),CHECK(response_expires_at=completed_at+interval '7 days'),
 CHECK((resource_type='BINDING' AND binding_id IS NOT NULL AND feedback_id IS NULL AND route='/api/v1/devices/bind') OR
 (resource_type='FEEDBACK' AND feedback_id IS NOT NULL AND binding_id IS NULL AND route='/api/v1/feedbacks')),
 CHECK(response_status IS NOT NULL),
 CHECK((state='COMPLETED' AND response_body IS NOT NULL AND jsonb_typeof(response_body)='object') OR (state='EXPIRED' AND response_body IS NULL))
);
CREATE INDEX idempotency_expiry ON rideguard.idempotency_records(response_expires_at);
CREATE INDEX idempotency_binding ON rideguard.idempotency_records(binding_id);
CREATE INDEX idempotency_feedback ON rideguard.idempotency_records(feedback_id);
CREATE TABLE rideguard.device_reports (
 id uuid PRIMARY KEY,binding_id uuid NOT NULL REFERENCES rideguard.device_bindings(id),observed_at timestamptz NOT NULL,received_at timestamptz NOT NULL,
 firmware_version varchar(64) CHECK(length(btrim(firmware_version))>0),
 radar_status varchar(16) NOT NULL CHECK(radar_status IN ('OK','ERROR','UNAVAILABLE','UNKNOWN')),
 imu_status varchar(16) NOT NULL CHECK(imu_status IN ('OK','ERROR','UNAVAILABLE','UNKNOWN')),
 display_status varchar(16) NOT NULL CHECK(display_status IN ('OK','ERROR','UNAVAILABLE','UNKNOWN')),
 vibration_status varchar(16) NOT NULL CHECK(vibration_status IN ('OK','ERROR','UNAVAILABLE','UNKNOWN')),
 battery_percent smallint CHECK(battery_percent BETWEEN 0 AND 100),accepted boolean NOT NULL,
 CHECK(observed_at BETWEEN received_at-interval '24 hours' AND received_at+interval '5 minutes')
);
CREATE UNIQUE INDEX report_accepted_time ON rideguard.device_reports(binding_id,observed_at) WHERE accepted;
CREATE INDEX report_latest ON rideguard.device_reports(binding_id,observed_at DESC,id DESC) WHERE accepted;
CREATE INDEX report_retention ON rideguard.device_reports(received_at);
CREATE INDEX report_binding ON rideguard.device_reports(binding_id);
