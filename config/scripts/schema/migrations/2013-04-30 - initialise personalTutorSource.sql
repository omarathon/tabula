-- Initialise personalTutorSource to "local" for all departments.
-- Some departments will have the value reset to "SITS".

update department
set settings = to_nclob(replace(dbms_lob.substr(settings, length(settings), 1), '"showStudentName":false','"showStudentName":false,"personalTutorSource":"local"'))
;

update department
set settings = to_nclob(replace(dbms_lob.substr(settings, length(settings), 1), '"showStudentName":true','"showStudentName":true,"personalTutorSource":"local"'))
;
