create type attraction_type as enum ('ARTIST', 'FOOD', 'EXHIBITION', 'DISCUSSION', 'EVENT', 'RIDE');

create table attraction
(
    id          bigserial       not null
        constraint pk_artist primary key,
    type        attraction_type not null,
    name        varchar(255)    not null,
    description text,
    website     varchar(255),
    instagram   varchar(255),
    facebook    varchar(255),
    youtube     varchar(255)
);
