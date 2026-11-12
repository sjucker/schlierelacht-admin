-- Only active attractions are served by the public API (list, detail, files, programm, events). Existing ones stay
-- visible.
alter table attraction
    add column active boolean not null default true;
