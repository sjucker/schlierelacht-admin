create table attraction_file
(
    id            bigserial    not null
        constraint pk_attraction_file primary key,
    attraction_id bigint       not null
        constraint fk_attraction_file_attraction references attraction (id) on delete cascade,
    filename      varchar(255) not null,
    filetype      varchar(100) not null,
    description   varchar(255) not null,
    filesize      bigint       not null,
    file_data     bytea        not null,
    uploaded_at   timestamp    not null default now(),
    uploaded_by   varchar(255) not null
);
