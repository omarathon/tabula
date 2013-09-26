alter table moduleregistration add occurrence nvarchar2(10);

drop index idx_modreg_notional_key;

create unique index idx_modreg_notional_key on moduleregistration(scjcode, modulecode, academicyear, cats, occurrence);
