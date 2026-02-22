create table image
(
    id            bigserial    not null
        constraint pk_image primary key,
    cloudflare_id varchar(255) not null
        constraint uq_image unique,
    description   varchar(255) not null,
    width         integer      not null,
    height        integer      not null
);

create type image_type as enum ('MAIN', 'ADDITIONAL');

create table attraction_image
(
    attraction_id bigint     not null,
    image_id  bigint     not null,
    type      image_type not null,
    constraint fk_attraction_image_attraction foreign key (attraction_id) references attraction (id) on delete cascade,
    constraint fk_attraction_image_image foreign key (image_id) references image (id) on delete cascade
);
