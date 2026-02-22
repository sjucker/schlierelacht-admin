create table programm
(
    id            bigserial not null
        constraint pk_programm primary key,
    attraction_id bigint    not null,
    location_id   bigint,
    from_date     date      not null,
    from_time     time      not null,
    to_date       date,
    to_time       time,
    constraint fk_programm_attraction foreign key (attraction_id) references attraction (id) on delete cascade,
    constraint fk_programm_location foreign key (location_id) references location (id) on delete set null
);
