-- Enforce: an attraction has at most one MAIN image (0 or 1). Additional images stay unlimited.

-- Resolve any pre-existing duplicates first (legacy data from before the admin UI
-- guaranteed this): keep the most recently linked main image, demote the rest to
-- ADDITIONAL so no image is lost.
update attraction_image ai
set type = 'ADDITIONAL'
where ai.type = 'MAIN'
  and ai.image_id <> (select max(ai2.image_id)
                      from attraction_image ai2
                      where ai2.attraction_id = ai.attraction_id
                        and ai2.type = 'MAIN');

-- Hard guarantee at the DB level: at most one MAIN row per attraction.
create unique index uq_attraction_one_main_image
    on attraction_image (attraction_id)
    where type = 'MAIN';
