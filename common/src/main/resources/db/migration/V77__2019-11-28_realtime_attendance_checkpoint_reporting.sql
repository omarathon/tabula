alter table attendancemonitoringcheckpoint
    add column needsSynchronisingToSits boolean not null default false,
    add column lastSynchronisedToSits timestamp(6);
