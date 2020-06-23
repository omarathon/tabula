alter table gradeboundary add column attempt int;
update gradeboundary set attempt = 1 where process = 'SAS';
update gradeboundary set attempt = 1 where process = 'RAS'; -- Let this be updated by the importer
