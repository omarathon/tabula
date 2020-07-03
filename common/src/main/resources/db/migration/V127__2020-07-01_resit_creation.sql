create table recordedresit (
    id varchar not null,
    hib_version numeric default 0,
    spr_code varchar not null,
    module_code varchar not null,
    academic_year smallint not null,
    occurrence varchar not null,
    sequence varchar not null,
    resit_sequence varchar not null,
    current_resit_attempt smallint not null,
    assessment_type varchar not null,
    marks_code varchar not null,
    weighting varchar not null,
    updated_by varchar not null,
    updated_date timestamp(6) not null,
    needs_writing_to_sits boolean default false,
    last_written_to_sits timestamp(6),
    constraint pk_recordedresit primary key (id)
);

create unique index ck_recordedresit on recordedresit (spr_code, academic_year, module_code, sequence);
