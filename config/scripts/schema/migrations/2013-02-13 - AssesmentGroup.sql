CREATE TABLE assessmentgroup (
	id nvarchar2(255) not null,
	assignment_id nvarchar2(255) not null,
	upstream_id nvarchar2(255) not null,
	occurrence nvarchar2(255)
);

-- generates entries for the tab assessmentgroup table to replace the implicit link
-- provided by the old occurrence and upstream_id fields on assignment
-- I recommend that you run the commented out query below and then the main query without the insert line
-- If the number of results is different then something is wrong!
-- select count(*) from assignment where upstream_id is not null and deleted = 0;

insert into assessmentgroup (id, assignment_id, upstream_id, occurrence)
select lower(sys_guid()), a.id as assignment_id, u.id as upstream_id, a.occurrence
from assignment a, upstreamassignment u
where a.deleted = 0
and a.upstream_id = u.id;

commit;
