create table push_device_token
(
    id         bigserial    not null
        constraint pk_push_device_token primary key,
    token      varchar(255) not null
        constraint uq_push_device_token_token unique,
    platform   varchar(16)  not null,
    locale     varchar(8),
    created_at timestamp    not null default now(),
    updated_at timestamp    not null default now()
);
