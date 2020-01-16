alter table RecipientNotificationInfo
    add column mywarwick_activity_sent boolean default false;

-- don't update any existing data as it is _so_ slow on prod
