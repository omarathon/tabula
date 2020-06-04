--      _.-'-'--._
--    ,', ~'` ( .'`.
--   ( ~'_ , .'(  >-)
--  ( .-' (  `__.-<  )
--   ( `-..--'_   .-')
--    `(_( (-' `-'.-)
--        `-.__.-'=/
--           `._`='
--              \\      g a l a x y   b r a i n
--               \\

create view studentcoursedetails_moduleregistration as
select
    scd.scjcode as scjcode,
    mr.id as module_registration_id
from studentcoursedetails scd
    join moduleregistration mr on scd.sprcode = mr.sprcode;

create rule studentcoursedetails_moduleregistration_noinsert as
    on insert to studentcoursedetails_moduleregistration
    do instead nothing;
create rule studentcoursedetails_moduleregistration_noupdate as
    on update to studentcoursedetails_moduleregistration
    do instead nothing;
create rule studentcoursedetails_moduleregistration_nodelete as
    on delete to studentcoursedetails_moduleregistration
    do instead nothing;
