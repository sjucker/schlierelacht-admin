alter table sponsoring
    add column sort_order integer not null default 0;

-- seed existing rows with current alphabetical order within each type
with ordered as (select id, row_number() over (partition by type order by name) - 1 as rn
                 from sponsoring)
update sponsoring s
set sort_order = o.rn
from ordered o
where s.id = o.id;
