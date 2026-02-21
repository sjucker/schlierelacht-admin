create type location_type as enum ('STAGE', 'FOOD_STAND', 'BAR', 'TENT', 'ATTRACTION');

create table location
(
    id            bigserial       not null
        constraint pk_location primary key,
    external_id   varchar(255)    not null
        constraint uq_location_external_id unique,
    type          location_type   not null,
    name          varchar(255)    not null,
    latitude      numeric(18, 14) not null,
    longitude     numeric(18, 14) not null,
    sort_order    integer         not null default 0,
    cloudflare_id varchar(255),
    map_id        varchar(5)
);
