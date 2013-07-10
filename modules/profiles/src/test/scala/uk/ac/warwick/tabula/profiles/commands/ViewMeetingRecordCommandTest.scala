package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent, ProfileService, ProfileServiceComponent}
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}

class ViewMeetingRecordCommandTest extends TestBase with Mockito{

	@Test
	def listsAllTutorMeetingsForRequestedStudentAndCurrentUser{
		withUser("test"){

			val studentCourseDetails = new StudentCourseDetails()
	    val requestor = new StaffMember()
	 		val relationship = new StudentRelationship()

		val meeting = new MeetingRecord
		val command = new ViewMeetingRecordCommandInternal(studentCourseDetails, currentUser,RelationshipType.PersonalTutor)
			with RelationshipServiceComponent with ProfileServiceComponent with MeetingRecordDaoComponent{
				var profileService = mock[ProfileService]
				var relationshipService = mock[RelationshipService]
			  var meetingRecordDao = mock[MeetingRecordDao]
		}
		// these are the calls we expect the applyInternal method to make
		command.relationshipService.getRelationships(RelationshipType.PersonalTutor, studentCourseDetails.sprCode) returns Seq(relationship)
	  command.profileService.getMemberByUniversityId(currentUser.universityId) returns Some(requestor)
		command.meetingRecordDao.list(Set(relationship), requestor) returns  Seq(meeting)


		command.applyInternal()  should be(Seq(meeting))
	}}

	@Test
	def listsAllSupervisorMeetingsForRequestedStudentAndCurrentUser{
		withUser("test"){

			val studentCourseDetails = new StudentCourseDetails()
			val requestor = new StaffMember()
			val relationship = new StudentRelationship()

			val meeting = new MeetingRecord
			val command = new ViewMeetingRecordCommandInternal(studentCourseDetails, currentUser,RelationshipType.Supervisor)
				with RelationshipServiceComponent with ProfileServiceComponent with MeetingRecordDaoComponent{
				var profileService = mock[ProfileService]
				var relationshipService = mock[RelationshipService]
				var meetingRecordDao = mock[MeetingRecordDao]
			}

			// these are the calls we expect the applyInternal method to make
			command.relationshipService.getRelationships(RelationshipType.Supervisor, studentCourseDetails.sprCode) returns Seq(relationship)
			command.profileService.getMemberByUniversityId(currentUser.universityId) returns Some(requestor)
			command.meetingRecordDao.list(Set(relationship), requestor) returns  Seq(meeting)


			command.applyInternal()  should be(Seq(meeting))
		}}



  @Test
  def  requiresReadTutorMeetingRecordPermissionIfRelationIsTutor(){
		withUser("test"){
    val perms =  new ViewMeetingRecordCommandPermissions with ViewMeetingRecordCommandState{
			val studentCourseDetails = new StudentCourseDetails()
			val requestingUser =  currentUser
			val relationshipType = RelationshipType.PersonalTutor
		}

		val permsChecker = mock[PermissionsChecking]

    perms.permissionsCheck(permsChecker)
		there was one(permsChecker).PermissionCheck(Permissions.Profiles.PersonalTutor.MeetingRecord.Read,perms.studentCourseDetails)
  }}

	@Test
	def  requiresReadSupervisorMeetingRecordPermissionIfRelationIsSupervisor(){
		withUser("test"){
		val perms =  new ViewMeetingRecordCommandPermissions with ViewMeetingRecordCommandState{
			val studentCourseDetails = new StudentCourseDetails()
			val requestingUser =  currentUser
			val relationshipType = RelationshipType.Supervisor
		}

		val permsChecker = mock[PermissionsChecking]

		perms.permissionsCheck(permsChecker)
		there was one(permsChecker).PermissionCheck(Permissions.Profiles.Supervisor.MeetingRecord.Read,perms.studentCourseDetails)
	}}

}
