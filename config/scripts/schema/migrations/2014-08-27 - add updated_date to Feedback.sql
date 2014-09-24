-- TAB-2707
alter table Feedback add (
  updated_date timestamp
);

-- Post-deploy
update Feedback set updated_date = uploaded_date;

alter table Feedback modify (
  updated_date timestamp not null
);