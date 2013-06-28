package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.Permissions

class ViewMeetingRecordCommandPermissionsTest extends TestBase with MeetingRecordCommandPermissionsTests{

  @Test
  def  requiresReadTutorMeetingRecordPermissionIfRelationIsTutor(){
    withUser("test"){
    val cmd =  new ViewMeetingRecordCommand(student, currentUser)
    cmd.permissionsAllChecks.get(Permissions.Profiles.PersonalTutor.MeetingRecord.Read).get should be(Some(student))
  }}
  // View record does not currently support supervisor meetings. Add an extra test here when it does...
}
