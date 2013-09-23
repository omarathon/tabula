UPDATE markerfeedback SET state = 'InProgress' WHERE state = 'DownloadedByMarker';
commit;
