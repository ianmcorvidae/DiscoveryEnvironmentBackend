SET search_path = public, pg_catalog;

--
-- jobs table
--
CREATE TABLE jobs (
  id                uuid not null default uuid_generate_v1(), -- primary key
  batch_id          uuid, -- self-join foreign key
  submitter         character varying(512) not null,
  date_submitted    timestamp with time zone,
  date_started      timestamp with time zone,
  date_completed    timestamp with time zone,
  app_id            uuid not null,
  command_line      text not null,
  env_variables     text,
  exit_code         integer, -- nullable because the job might be running
  failure_threshold integer NOT NULL,
  failure_count     integer
);
