-- marks any RecordAttendance events that were recorded since 2020.2.1-1 (hotfix for TAB-8131) as `onlyIncludesChanges`
update auditevent set data = jsonb_set(data::jsonb, '{onlyIncludesChanges}', 'true')
    where eventtype = 'RecordAttendance' and eventstage = 'after' and eventdate > '2020-02-12 13:13:00';