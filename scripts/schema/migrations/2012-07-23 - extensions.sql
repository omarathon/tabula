create table Extension (
    id nvarchar2(255) not null,
    expiryDate timestamp,
    universityId nvarchar2(255) not null,
    userId nvarchar2(255) not null,
    assignment_id nvarchar2(255),
    reason nvarchar2(4000),
    approved number(1,0) not null,
    approvedOn timestamp,
    approvalComments nvarchar2(4000),
    constraint "EXTENSION_PK" PRIMARY KEY ("ID")
);

create index idx_extension_assignment on EXTENSION("ASSIGNMENT_ID");
create index idx_extension_user on EXTENSION("USERID");