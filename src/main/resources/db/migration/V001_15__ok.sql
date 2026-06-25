create type ok_team as enum (
    'PRAESIDIALES_STADT_KOMMUNIKATION_FINANZEN',
    'BAU_INFRASTRUKTUR',
    'GASTRONOMIE',
    'PROGRAMM_AKTIVITAETEN',
    'SICHERHEIT',
    'SPONSORING');

create table ok_member
(
    id            bigserial    not null
        constraint pk_ok_member primary key,
    name          varchar(255) not null,
    role          varchar(255) not null,
    email         varchar(255),
    cloudflare_id varchar(255),
    sort_order    integer      not null default 0
);

create table ok_team_member
(
    id   bigserial    not null
        constraint pk_ok_team_member primary key,
    name varchar(255) not null,
    team ok_team      not null
);
