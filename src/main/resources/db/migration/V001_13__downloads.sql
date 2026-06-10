create type download_category as enum ('LOGOS', 'SPONSORING', 'GASTRONOMIE', 'PROGRAMM');

create table download
(
    id          bigserial         not null
        constraint pk_download primary key,
    uploaded_at timestamp         not null default now(),
    uploaded_by varchar(255)      not null,
    filename    varchar(255)      not null,
    filetype    varchar(100)      not null,
    description varchar(255)      not null,
    filesize    bigint            not null,
    category    download_category not null,
    file_data   bytea             not null
);
