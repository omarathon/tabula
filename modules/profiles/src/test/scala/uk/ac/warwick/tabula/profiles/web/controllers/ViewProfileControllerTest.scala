package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, MeetingRecord, RelationshipType, StudentMember}
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommandState
import uk.ac.warwick.tabula.services.{SmallGroupService, ProfileService, UserLookupService, SecurityService}
import uk.ac.warwick.tabula.permissions.{PermissionsTarget, Permission}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.userlookup.UserLookup

class ViewProfileControllerTest extends TestBase with Mockito{

	val controller = new ViewProfileController
	// need to have a security service defined or we'll get a NPE in PermissionsCheckingMethods.restricted()
	controller.securityService = mock[SecurityService]
	controller.smallGroupService = mock[SmallGroupService]

	val member = new StudentMember()
	val courseDetails = new StudentCourseDetails()
	courseDetails.mostSignificant = true
	member.studentCourseDetails.add(courseDetails)

	@Test def createsTutorMeetingListCommand(){
	withUser("test"){
		controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
		val cmd = controller.viewTutorMeetingRecordCommand(member).get.asInstanceOf[ViewMeetingRecordCommandState]
	  cmd.relationshipType should be(RelationshipType.PersonalTutor)
		}
	}


	@Test def createsSupervisorMeetingListCommand(){
	withUser("test"){
		controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
		val cmd = controller.viewSupervisorMeetingRecordCommand(member).get.asInstanceOf[ViewMeetingRecordCommandState]
		cmd.relationshipType should be(RelationshipType.Supervisor)
	}}

	@Test def exposesMeetingListsInModel(){
  withUser("test"){
		
	  val member = new StudentMember
		member.universityId = "1234"
		val viewProfileCommand = mock[Appliable[StudentMember]]
		viewProfileCommand.apply returns member

    val tutorMeetings = Seq(new MeetingRecord)
		val supervisorMeetings = Seq(new MeetingRecord)

		val tutorCommand = mock[Appliable[Seq[MeetingRecord]]]
		val supervisorCommand = mock[Appliable[Seq[MeetingRecord]]]
		tutorCommand.apply returns tutorMeetings
		supervisorCommand.apply returns supervisorMeetings

		// mocks necessary for base class functionality that should really be factored out
		controller.userLookup = mock[UserLookupService]
		controller.profileService = mock[ProfileService]
		controller.profileService.getMemberByUserId("test", true) returns Some(member)
		controller.smallGroupService.findSmallGroupsByStudent(member.asSsoUser) returns (Nil)

		val mav = controller.viewProfile(viewProfileCommand, Some(tutorCommand), Some(supervisorCommand),"test","test")

		mav.map("tutorMeetings") should be(tutorMeetings)
		mav.map("supervisorMeetings") should be(supervisorMeetings)
	}}
}