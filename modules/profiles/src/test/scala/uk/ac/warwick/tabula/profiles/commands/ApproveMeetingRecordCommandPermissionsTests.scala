package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.MeetingRecordApproval

class ApproveMeetingRecordCommandPermissionsTests extends TestBase  with MeetingRecordCommandPermissionsTests {


  @Test
  def requiresApproveTutorMeetingRecordPermissionIfRelationIsTutor{
    val approval = new MeetingRecordApproval
    approval.approver = relationship.studentMember
    approval.meetingRecord = tutorMeeting

    val cmd = new ApproveMeetingRecordCommand(approval)

    cmd.permissionsAllChecks.get(Permissions.Profiles.PersonalTutor.MeetingRecord.Update).get should be(Some(tutorMeeting))

  }

  @Test
  def requiresApproveSupervisorMeetingRecordPermissionIfRelationIsSupervisor{
    val approval = new MeetingRecordApproval
    approval.approver = relationship.studentMember
    approval.meetingRecord = supervisorMeeting

    val cmd = new ApproveMeetingRecordCommand(approval)

    cmd.permissionsAllChecks.get(Permissions.Profiles.Supervisor.MeetingRecord.Update).get should be(Some(supervisorMeeting))

  }

}
