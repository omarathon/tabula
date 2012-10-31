create table FeedbackTemplate (
    id nvarchar2(255) not null,
    name nvarchar2(255),
    description nvarchar2(4000),
    department_id nvarchar2(255),
    attachment_id nvarchar2(255),
    CONSTRAINT "FEEDBACKTEMPLATE_PK" PRIMARY KEY ("ID")
);

create index feedbacktemplate_department on feedbacktemplate(department_id);
create index feedbacktemplate_attachment on feedbacktemplate(attachment_id);