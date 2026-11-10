-- When a Meldung was last activated: the app shows this instead of the creation time, so messages can be prepared
-- in advance and published later. Null while a message has never been active.
alter table message
    add column published_at timestamp;

update message
set published_at = created_at
where active;
