package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import scala.Some
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.{StudentRelationship, Member}

class ModifyMeetingRecordCommandPermissionsTests extends TestBase  with MeetingRecordCommandPermissionsTests {


  @Test
  def requiresCreateTutorMeetingRecordPermissionIfRelationIsTutor(){
    val cmd =  new StubModifyMeetingRecordCommand(creator, relationship)
    cmd.permissionsAllChecks.get(Permissions.Profiles.PersonalTutor.MeetingRecord.Create).get should be(Some(relationship.studentMember))
  }

  @Test
  def requiresCreateSuperivisorMeetingRecordPermissionIfRelationIsSupervisor(){
    val cmd =  new StubModifyMeetingRecordCommand(creator, supervisorRelationship)
    cmd.permissionsAllChecks.get(Permissions.Profiles.Supervisor.MeetingRecord.Create).get should be(Some(relationship.studentMember))
  }
  class StubModifyMeetingRecordCommand(creator: Member, relationship: StudentRelationship)
    extends ModifyMeetingRecordCommand(creator, relationship) with StubCommand

}