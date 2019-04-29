update mitigatingcircumstancessubmission set contactOther = null;
alter table mitigatingcircumstancessubmission alter column contactOther type bytea using contactOther::bytea;