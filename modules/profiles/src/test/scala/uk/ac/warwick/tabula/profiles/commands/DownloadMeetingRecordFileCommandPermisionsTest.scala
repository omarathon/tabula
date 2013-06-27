package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.permissions.Permissions

class DownloadMeetingRecordFileCommandPermisionsTest  extends TestBase with MeetingRecordCommandPermisionsTests{

  @Test
  def  requiresReadDetailsTutorMeetingRecordPermissionIfRelationIsTutor(){
    val cmd =  new DownloadMeetingRecordFilesCommand(tutorMeeting)
    cmd.permissionsAllChecks.get(Permissions.Profiles.PersonalTutor.MeetingRecord.ReadDetails).get should be(Some(tutorMeeting))
  }

  @Test
  def  requiresReadDetailsSupervisorMeetingRecordPermissionIfRelationIsSupervisor(){
    val cmd =  new DownloadMeetingRecordFilesCommand(supervisorMeeting)
    cmd.permissionsAllChecks.get(Permissions.Profiles.Supervisor.MeetingRecord.ReadDetails).get should be(Some(supervisorMeeting))
  }

}
