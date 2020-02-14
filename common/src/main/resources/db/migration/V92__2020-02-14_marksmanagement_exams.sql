-- drop old exams as they have all been migrated to CM2
drop table exam;

create table exam(
     id varchar(255) not null,
     name varchar not null,
     startDateTime timestamp(6),
     deleted boolean not null default false,
     department_id varchar(255) not null,
     academicyear smallint not null,
     lastmodified timestamp(6) not null,
     constraint pk_exam primary key (id),
     constraint fk_exam_department foreign key (department_id) references department(id)
);

create index idx_exam_department on exam (department_id);
create index idx_exam_department_year on exam (department_id, academicyear);

create table examquestion(
    id varchar(255) not null,
    name varchar not null,
    score int,
    exam_id varchar(255) not null,
    parent varchar(255),
    constraint pk_examquestion primary key (id),
    constraint fk_examquestion_parent foreign key (parent) references examquestion(id),
    constraint fk_question_exam foreign key (exam_id) references exam(id)
);

create index idx_question_exam on examquestion (exam_id);
create index idx_question_parent on examquestion (parent);

create table examquestionrule(
    id varchar(255) not null,
    exam_id varchar(255) not null,
    min int,
    max int,
    constraint pk_examquestionrule primary key (id),
    constraint fk_examquestionrule_exam foreign key (exam_id) references exam(id)
);

create index idx_examquestionrule_exam on examquestion (exam_id);

create table examquestionruleset(
    question_id varchar(255) not null,
    rule_id varchar(255) not null,
    constraint fk_examquestionruleset_question foreign key (question_id) references examquestion(id),
    constraint fk_examquestionruleset_rule foreign key (rule_id) references examquestionrule(id)
);

create index idx_examquestionruleset_question on examquestionruleset (question_id);
create index idx_examquestionruleset_rule on examquestionruleset (rule_id);