alter table gradeboundary
    add column agreedStatus varchar,
    add column incrementsAttempt boolean;
update gradeboundary set agreedStatus = 'H', incrementsAttempt = false;
alter table gradeboundary
    alter column agreedStatus set not null,
    alter column incrementsAttempt set not null;
