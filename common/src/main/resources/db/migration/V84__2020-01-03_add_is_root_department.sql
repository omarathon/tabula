alter table department
    add column is_root_department boolean default true;
update department set is_root_department = false where parent_id is not null;

-- TAB-7964
update department set parent_id = (select id from department where code = 'ce') where code in ('c-nw', 'c-cy', 'c-wc', 'c-sl');
