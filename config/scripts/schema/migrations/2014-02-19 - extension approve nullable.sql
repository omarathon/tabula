-- TAB-1921 
-- need to remove approved, in the meantime - let's kill the not null constraint on it

alter table extension modify(approved null);