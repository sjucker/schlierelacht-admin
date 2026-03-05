create type sponsoring_type as enum ('HAUPTSPONSOREN', 'ORGANISATION', 'GASTREGION', 'GOLD', 'SILBER', 'BRONZE', 'GOENNER');

create table sponsoring
(
    id            bigserial       not null
        constraint pk_sponsoring primary key,
    type          sponsoring_type not null,
    name          varchar(255)    not null,
    cloudflare_id varchar(255)    not null,
    url           varchar(255)
);
