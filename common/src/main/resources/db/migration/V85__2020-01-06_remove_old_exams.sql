drop table exam;
drop table markscheme;
drop table marker_usergroup;
delete from feedback where discriminator = 'exam';
delete from formfield where fieldtype = 'marker';
alter table assignment drop column archived;

delete from grantedpermission where permission like 'ExamFeedback%' or permission like 'ExamMarkerFeedback%';
update grantedpermission set permission = 'Feedback.Publish' where permission = 'AssignmentFeedback.Publish';
update grantedpermission set permission = 'Feedback.UnPublish' where permission = 'AssignmentFeedback.UnPublish';
update grantedpermission set permission = 'Feedback.Rate' where permission = 'AssignmentFeedback.Rate';
update grantedpermission set permission = 'Feedback.Manage' where permission = 'AssignmentFeedback.Manage';
update grantedpermission set permission = 'Feedback.Read' where permission = 'AssignmentFeedback.Read';
update grantedpermission set permission = 'Feedback.DownloadMarksTemplate' where permission = 'AssignmentFeedback.DownloadMarksTemplate';
update grantedpermission set permission = 'MarkerFeedback.Manage' where permission = 'AssignmentMarkerFeedback.Manage';
update grantedpermission set permission = 'MarkerFeedback.DownloadMarksTemplate' where permission = 'AssignmentMarkerFeedback.DownloadMarksTemplate';

delete from roleoverride where permission like 'ExamFeedback%' or permission like 'ExamMarkerFeedback%';
update roleoverride set permission = 'Feedback.Publish' where permission = 'AssignmentFeedback.Publish';
update roleoverride set permission = 'Feedback.UnPublish' where permission = 'AssignmentFeedback.UnPublish';
update roleoverride set permission = 'Feedback.Rate' where permission = 'AssignmentFeedback.Rate';
update roleoverride set permission = 'Feedback.Manage' where permission = 'AssignmentFeedback.Manage';
update roleoverride set permission = 'Feedback.Read' where permission = 'AssignmentFeedback.Read';
update roleoverride set permission = 'Feedback.DownloadMarksTemplate' where permission = 'AssignmentFeedback.DownloadMarksTemplate';
update roleoverride set permission = 'MarkerFeedback.Manage' where permission = 'AssignmentMarkerFeedback.Manage';
update roleoverride set permission = 'MarkerFeedback.DownloadMarksTemplate' where permission = 'AssignmentMarkerFeedback.DownloadMarksTemplate';