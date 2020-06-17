create table progressiondecision (
  id varchar not null,
  spr_code varchar not null,
  sequence varchar not null,
  academic_year smallint not null,
  outcome varchar not null,
  notes text,
  resit_period boolean not null,
  constraint pk_progressiondecision primary key (id)
);

create unique index ck_progressiondecision on progressiondecision (spr_code, sequence);

create view studentcoursedetails_progressiondecision as
select
    scd.scjcode as scjcode,
    pd.id as progression_decision_id
from studentcoursedetails scd
    join progressiondecision pd on scd.sprcode = pd.spr_code;

create rule studentcoursedetails_progressiondecision_noinsert as
    on insert to studentcoursedetails_progressiondecision
    do instead nothing;
create rule studentcoursedetails_progressiondecision_noupdate as
    on update to studentcoursedetails_progressiondecision
    do instead nothing;
create rule studentcoursedetails_progressiondecision_nodelete as
    on delete to studentcoursedetails_progressiondecision
    do instead nothing;
