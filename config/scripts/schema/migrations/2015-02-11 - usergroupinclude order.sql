-- TAB-3064
ALTER TABLE usergroupstatic
ADD (
	id nvarchar2(255),
	position number(5, 0)
);

-- set a GUID for all of the existing rows (note oracle generateed GUID's are often sequential so may look almost identical)
UPDATE usergroupstatic SET id = sys_guid();

-- set the id as a primary key once all rows have a value
ALTER TABLE usergroupstatic ADD CONSTRAINT usergroupstatic_pk PRIMARY KEY (id);
ALTER TABLE usergroupstatic ADD CONSTRAINT usergroupstatic_nn CHECK(id IS NOT NULL) ;