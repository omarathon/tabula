delete from membernote where discriminator != 'note';

alter table membernote drop column start_date, drop column end_date;