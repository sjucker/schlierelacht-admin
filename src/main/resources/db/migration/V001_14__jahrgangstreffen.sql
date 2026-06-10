create type meetup_jahrgang as enum (
    'BEFORE_1954',
    'BORN_1954_1958',
    'BORN_1959_1963',
    'BORN_1965_1968',
    'BORN_1969_1973',
    'BORN_1974_1978',
    'BORN_1979_1983',
    'BORN_1984_1988',
    'BORN_1989_1993',
    'BORN_1994_1998',
    'AFTER_1998'
    );

create table meetup_registration
(
    id            bigserial       not null
        constraint pk_meetup_registration primary key,
    registered_at timestamp       not null default now(),
    firstname     varchar(255)    not null,
    lastname      varchar(255)    not null,
    email         varchar(255)    not null,
    jahrgang      meetup_jahrgang not null,
    show_on_list  boolean         not null default true
);
