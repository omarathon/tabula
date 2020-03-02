alter table upstreamassignment rename to assessmentcomponent;
create view upstreamassignment as select * from assessmentcomponent;

