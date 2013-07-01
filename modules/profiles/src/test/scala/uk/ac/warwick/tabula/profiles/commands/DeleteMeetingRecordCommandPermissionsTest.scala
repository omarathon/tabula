package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.permissions.Permissions

class DeleteMeetingRecordCommandPermissionsTest extends TestBase with MeetingRecordCommandPermissionsTests {

  @Test
  def requiresDeleteTutorMeetingRecordPermissionIfRelationIsTutor {
    withUser("test") {
      val cmd = new DeleteMeetingRecordCommand(tutorMeeting, currentUser)

      cmd.permissionsAllChecks.get(Permissions.Profiles.PersonalTutor.MeetingRecord.Delete).get should be(Some(tutorMeeting))
    }
  }
  @Test
  def requiresDeleteSupervisorMeetingRecordPermissionIfRelationIsSupervisor{
    withUser("test") {
      val cmd = new DeleteMeetingRecordCommand(supervisorMeeting, currentUser)
      cmd.permissionsAllChecks.get(Permissions.Profiles.Supervisor.MeetingRecord.Delete).get should be(Some(supervisorMeeting))
    }
  }

}
