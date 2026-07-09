create table gallery_image
(
    id            bigserial    not null
        constraint pk_gallery_image primary key,
    category      varchar(255) not null,
    cloudflare_id varchar(255) not null,
    uploaded_at   timestamp    not null default now(),
    uploaded_by   varchar(255) not null
);

create index idx_gallery_image_category on gallery_image (category);

-- Admin-controlled ordering for gallery categories. Categories themselves stay free-text on gallery_image;
-- this table only carries their display order for the public /api/gallery endpoint.
create table gallery_category
(
    id         bigserial    not null
        constraint pk_gallery_category primary key,
    name       varchar(255) not null
        constraint uq_gallery_category_name unique,
    sort_order integer      not null default 0
);
