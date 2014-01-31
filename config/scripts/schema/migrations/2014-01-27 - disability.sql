--- TAB-128

alter table member add disability nvarchar2(20);

create table disability
(	code nvarchar2(20) not null enable,
	shortName nvarchar2(20),
	sitsDefinition nvarchar2(300),
	tabulaDefinition nvarchar(100),
	lastUpdatedDate timestamp(6),
  constraint disability_pk primary key (code)
);

/* set our own consistent text for disabilities on which we want to report */
update disability set tabulaDefinition = 'Learning difficulty' where code = '01';
update disability set tabulaDefinition = 'Blind/vision impaired' where code = '02';
update disability set tabulaDefinition = 'Deaf/hearing impaired' where code = '03';
update disability set tabulaDefinition = 'Mobility issues' where code = '04';
update disability set tabulaDefinition = 'Personal care support needs' where code = '05';
update disability set tabulaDefinition = 'Mental health condition' where code = '06';
update disability set tabulaDefinition = 'Unseen disability' where code = '07';
update disability set tabulaDefinition = 'Multiple disabilities' where code = '08';
update disability set tabulaDefinition = 'Unclassified disability' where code = '09';
update disability set tabulaDefinition = 'Autistic Spectrum Disorder' where code = '10';
update disability set tabulaDefinition = 'Learning difficulty' where code = '11';
update disability set tabulaDefinition = 'Unclassified disability' where code = '96';
update disability set tabulaDefinition = 'Autistic Spectrum Disorder' where code = 'B';
update disability set tabulaDefinition = 'Blind/vision impaired' where code = 'C';
update disability set tabulaDefinition = 'Deaf/hearing impaired' where code = 'D';
update disability set tabulaDefinition = 'Long standing condition' where code = 'E';
update disability set tabulaDefinition = 'Mental health condition' where code = 'F';
update disability set tabulaDefinition = 'Learning difficulty' where code = 'G';
update disability set tabulaDefinition = 'Mobility issues' where code = 'H';
update disability set tabulaDefinition = 'Unclassified disability' where code = 'I';
update disability set tabulaDefinition = 'Multiple disabilities' where code = 'J';
update disability set tabulaDefinition = 'Autistic Spectrum Disorder' where code = 'T';

/* empty strings (deliberately not null) for things we consider to be NOT disabilities */
update disability set tabulaDefinition = '' where code = '00';
update disability set tabulaDefinition = '' where code = '97';
update disability set tabulaDefinition = '' where code = '98';
update disability set tabulaDefinition = '' where code = '99';
update disability set tabulaDefinition = '' where code = 'A';
update disability set tabulaDefinition = '' where code = 'N';
update disability set tabulaDefinition = '' where code = 'W';
