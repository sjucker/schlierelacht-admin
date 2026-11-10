create type message_priority as enum ('HIGH', 'MEDIUM', 'LOW');

-- Meldungen shown in the app: the most important active one on the home screen, all active ones in a list.
create table message
(
    id         bigserial        not null
        constraint pk_message primary key,
    priority   message_priority not null,
    title      varchar(255)     not null,
    content    text             not null,
    active     boolean          not null default false,
    created_by varchar(255)     not null,
    created_at timestamp        not null default now()
);
