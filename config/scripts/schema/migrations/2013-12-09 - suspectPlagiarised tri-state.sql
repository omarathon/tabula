alter table Submission add plagiarismInvestigation nvarchar2(50) default 'NotInvestigated';

update Submission set plagiarismInvestigation = 'SuspectPlagiarised' where suspectplagiarised = 1;

alter table Submission set unused column suspectplagiarised;