create table tag
(
    id   bigserial       not null
        constraint pk_tag primary key,
    name varchar(255)    not null,
    type attraction_type not null
);

create table attraction_tag
(
    attraction_id bigint not null,
    tag_id        bigint not null,
    constraint pk_attraction_tag primary key (attraction_id, tag_id),
    constraint fk_attraction_tag_attraction foreign key (attraction_id) references attraction (id) on delete cascade,
    constraint fk_attraction_tag_tag foreign key (tag_id) references tag (id) on delete cascade
);
