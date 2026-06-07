create table news
(
    id            bigserial    not null constraint pk_news primary key,
    date          date         not null,
    title         varchar(255) not null,
    intro_text    text         not null,
    full_text     text         not null,
    cloudflare_id varchar(255),
    active        boolean      not null default true
);
