--- TAB-1539
--- This is all commented out because we don't want to run it multiple times and it's not strictly a migration!?

/*
update assignment set closedate = closedate + interval '1' hour
where createddate >= '12-SEP-13 14:50:00' and (
  (closedate between '31-MAR-13 01:00:00' and '27-OCT-13 02:00:00') or
  (closedate between '30-MAR-14 01:00:00' and '26-OCT-13 02:00:00')
);

update assignment set closedate = closedate + interval '1' hour
where (
  (closedate between '31-MAR-13 01:00:00' and '27-OCT-13 02:00:00') or
  (closedate between '30-MAR-14 01:00:00' and '26-OCT-13 02:00:00')
) and (
  closedate like '%22.59.59%' or
  closedate like '%23.00.00%' or
  closedate like '%11.00.00%'
);

update extension set expiryDate = expiryDate + interval '1' hour
where (requestedon >= '12-SEP-13 14:50:00' or approvedon >= '12-SEP-13 14:50:00') and (
  (expiryDate between '31-MAR-13 01:00:00' and '27-OCT-13 02:00:00') or
  (expiryDate between '30-MAR-14 01:00:00' and '26-OCT-13 02:00:00')
);

update extension set requestedExpiryDate = requestedExpiryDate + interval '1' hour
where (requestedon >= '12-SEP-13 14:50:00' or approvedon >= '12-SEP-13 14:50:00') and (
  (requestedExpiryDate between '31-MAR-13 01:00:00' and '27-OCT-13 02:00:00') or
  (requestedExpiryDate between '30-MAR-14 01:00:00' and '26-OCT-13 02:00:00')
);

update Assignment set openDate = openDate + interval '1' hour where openDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Assignment set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update FileAttachment set dateUploaded = dateUploaded + interval '1' hour where dateUploaded between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Feedback set uploaded_date = uploaded_date + interval '1' hour where uploaded_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Feedback set released_date = released_date + interval '1' hour where released_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Submission set submitted_date = submitted_date + interval '1' hour where submitted_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Job set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Job set updatedDate = updatedDate + interval '1' hour where updatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update OriginalityReport set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Extension set approvedOn = approvedOn + interval '1' hour where approvedOn between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Extension set requestedOn = requestedOn + interval '1' hour where requestedOn between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Member set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MarkerFeedback set uploaded_date = uploaded_date + interval '1' hour where uploaded_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update StudentRelationship set uploaded_date = uploaded_date + interval '1' hour where uploaded_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update StudentRelationship set start_date = start_date + interval '1' hour where start_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update StudentRelationship set end_date = end_date + interval '1' hour where end_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update SitsStatus set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update ModeOfAttendance set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MeetingRecord set creation_date = creation_date + interval '1' hour where creation_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MeetingRecord set last_updated_date = last_updated_date + interval '1' hour where last_updated_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MeetingRecord set meeting_date = meeting_date + interval '1' hour where meeting_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MeetingRecordApproval set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MeetingRecordApproval set creation_date = creation_date + interval '1' hour where creation_date between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update StudentCourseDetails set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update StudentCourseYearDetails set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update Course set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringPointSet set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringPointSet set updatedDate = updatedDate + interval '1' hour where updatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringPoint set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringPoint set updatedDate = updatedDate + interval '1' hour where updatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringCheckpoint set createdDate = createdDate + interval '1' hour where createdDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MonitoringCheckpoint set updatedDate = updatedDate + interval '1' hour where updatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update ModuleRegistration set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MemberNote set creationDate = creationDate + interval '1' hour where creationDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
update MemberNote set lastUpdatedDate = lastUpdatedDate + interval '1' hour where lastUpdatedDate between '12-SEP-13 14:50:00' and '27-OCT-13 02:00:00';
*/