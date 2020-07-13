create table studentaward (
  id varchar not null,
  spr_code varchar not null,
  academic_year smallint not null,
  awardcode varchar(20) not null,
  classification varchar not null,
  award_date timestamp(6) not null,
  constraint pk_studentaward primary key (id)
);
create unique index ck_studentaward on studentaward (spr_code, academic_year, awardcode);

create view progressiondecision_studentaward as
select
    pd.id as progression_decision_id,
    sawd.id as student_award_id
from progressiondecision pd
    join studentaward sawd  on sawd.spr_code = pd.spr_code
    and sawd.academic_year = pd.academic_year;

create rule progressiondecision_studentaward_noinsert as
    on insert to progressiondecision_studentaward
    do instead nothing;
create rule studentaward_progressiondecision_noupdate as
    on update to progressiondecision_studentaward
    do instead nothing;
create rule progressiondecision_studentaward_nodelete as
    on delete to progressiondecision_studentaward
    do instead nothing;

