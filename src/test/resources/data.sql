--alter table formfield add column 
--    position integer not null
--;





insert into usergroup (id,universityids) values ('1',0);
insert into usergroupinclude (group_id, usercode) values ('1', 'cusebr')
insert into usergroupinclude (group_id, usercode) values ('1', 'cusfal')

insert into department (id,code,name,collectFeedbackRatings,ownersgroup_id) values ('1','CS','Computer Science',1,'1');
insert into department (id,code,name,collectFeedbackRatings) values ('2','CH','Chemistry',1);

insert into module (id,department_id,code,name,active) values ('1','1','CS108','Introduction to Programming',1);
insert into module (id,department_id,code,name,active) values ('2','1','CS240','History of Computing',1);

-- set up an assignment for the "Intro to Programming" module
insert into assignment(id, name, module_id, academicyear, active, attachmentlimit, 
	collectmarks,deleted,collectsubmissions,restrictsubmissions,
	allowlatesubmissions,allowresubmission,displayplagiarismnotice,archived,createdDate)
	values ('1','Test Assignment','1','2011',1,1,1,1,1,1,1,1,1,1,sysdate);

-- set up an assignment for the "Intro to Programming" module
insert into assignment(id, name, module_id, academicyear, active, attachmentlimit, 
	collectmarks,deleted,collectsubmissions,restrictsubmissions,
	allowlatesubmissions,allowresubmission,displayplagiarismnotice,archived,createdDate)
	values ('2','Test Computing Assignment','1','2011',1,1,1,1,1,1,1,1,1,1,sysdate);
	
-- set up an assignment for the "History of Computing" module
insert into assignment(id, name, module_id, academicyear, active, attachmentlimit, 
	collectmarks,deleted,collectsubmissions,restrictsubmissions,
	allowlatesubmissions,allowresubmission,displayplagiarismnotice,archived,createdDate)
	values ('3','Programming Assignment','2','2011',1,1,1,1,1,1,1,1,1,1,sysdate);



