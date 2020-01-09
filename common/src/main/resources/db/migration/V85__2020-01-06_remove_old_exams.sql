delete from feedback where discriminator = 'exam';
delete from formfield where fieldtype = 'marker';

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

update notification set notification_type = 'Cm2MarkedPlagiarised' where notification_type = 'MarkedPlagarised';
update notification set notification_type = 'Cm2ReleaseToMarker' where notification_type = 'ReleaseToMarker';
update notification set notification_type = 'Cm2StudentFeedbackAdjustment' where notification_type = 'StudentFeedbackAdjustment';
update notification set notification_type = 'Cm2RequestAssignmentAccess' where notification_type = 'RequestAssignmentAccess';
update notification set notification_type = 'CM2ReturnToMarker' where notification_type = 'ReturnToMarker';