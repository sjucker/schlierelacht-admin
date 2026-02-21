create table login
(
    email      varchar(255) not null
        constraint login_pk primary key,
    name       varchar(255) not null,
    password   varchar(255) not null,
    active     boolean      not null,
    last_login timestamp    null
);
