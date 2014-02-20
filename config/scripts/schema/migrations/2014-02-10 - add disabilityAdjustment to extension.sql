-- TAB-882

alter table extension
add (disabilityAdjustment number(1,0) default 0 not null);

create index idx_extn_disability_adjust on extension (disabilityAdjustment);