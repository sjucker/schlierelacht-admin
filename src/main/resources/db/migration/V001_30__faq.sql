-- FAQ for the app: entries with the same group_name are shown together; groups ordered by their lowest sort_order,
-- entries within a group by sort_order, then title. Only active entries are served.
create table faq
(
    id         bigserial    not null
        constraint pk_faq primary key,
    group_name varchar(255) not null,
    title      varchar(255) not null,
    content    text         not null,
    sort_order integer      not null default 0,
    active     boolean      not null default false
);
