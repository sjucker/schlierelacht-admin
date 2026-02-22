alter table attraction
    add column external_id varchar(255) not null default '',
    add constraint uq_attraction unique (external_id);
