alter table mitigatingcircumstancessubmission
    add column contacted boolean,
    add column contacts varchar(255)[],
    add column contactOther text,
    add column noContactReason bytea
;