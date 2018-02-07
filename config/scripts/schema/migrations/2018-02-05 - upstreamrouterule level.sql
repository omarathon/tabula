alter table upstreamrouterule add (levelcode nvarchar2(10));
update upstreamrouterule set levelcode = yearofstudy;
alter table upstreamrouterule set unused column yearofstudy;
alter table upstreamrouterule drop unused columns;