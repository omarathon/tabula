--- TAB-128

alter table member add disability nvarchar2(20);

create table disability
(	code nvarchar2(20) not null enable,
	shortName nvarchar2(20),
	sitsDefinition nvarchar2(300),
	tabulaDefinition nvarchar2(30),
	lastUpdatedDate timestamp(6),
  constraint disability_pk primary key (code)
);

/* set our own consistent text for disabilities on which we want to report */
insert into disability  (code, tabulaDefinition) values ('01', 'Learning difficulty');
insert into disability  (code, tabulaDefinition) values ('02', 'Blind/vision impaired');
insert into disability  (code, tabulaDefinition) values ('03', 'Deaf/hearing impaired');
insert into disability  (code, tabulaDefinition) values ('04', 'Mobility issues');
insert into disability  (code, tabulaDefinition) values ('05', 'Personal care needs');
insert into disability  (code, tabulaDefinition) values ('06', 'Mental health condition');
insert into disability  (code, tabulaDefinition) values ('07', 'Unseen disability');
insert into disability  (code, tabulaDefinition) values ('08', 'Multiple disabilities');
insert into disability  (code, tabulaDefinition) values ('09', 'Unclassified disability');
insert into disability  (code, tabulaDefinition) values ('10', 'Autistic Spectrum Disorder');
insert into disability  (code, tabulaDefinition) values ('11', 'Learning difficulty');
insert into disability  (code, tabulaDefinition) values ('96', 'Unclassified disability');
insert into disability  (code, tabulaDefinition) values ('B', 'Autistic Spectrum Disorder');
insert into disability  (code, tabulaDefinition) values ('C', 'Blind/vision impaired');
insert into disability  (code, tabulaDefinition) values ('D', 'Deaf/hearing impaired');
insert into disability  (code, tabulaDefinition) values ('E', 'Long standing condition');
insert into disability  (code, tabulaDefinition) values ('F', 'Mental health condition');
insert into disability  (code, tabulaDefinition) values ('G', 'Learning difficulty');
insert into disability  (code, tabulaDefinition) values ('H', 'Mobility issues');
insert into disability  (code, tabulaDefinition) values ('I', 'Unclassified disability');
insert into disability  (code, tabulaDefinition) values ('J', 'Multiple disabilities');
insert into disability  (code, tabulaDefinition) values ('T', 'Autistic Spectrum Disorder');

/* use a ~magic string~ for things we consider to be NOT disabilities */
insert into disability (code, tabulaDefinition) values ('00', 'NONE');
insert into disability (code, tabulaDefinition) values ('97', 'NONE');
insert into disability (code, tabulaDefinition) values ('98', 'NONE');
insert into disability (code, tabulaDefinition) values ('99', 'NONE');
insert into disability (code, tabulaDefinition) values ('A', 'NONE');
insert into disability (code, tabulaDefinition) values ('N', 'NONE');
insert into disability (code, tabulaDefinition) values ('W', 'NONE');
