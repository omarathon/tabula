package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommandState
import uk.ac.warwick.tabula.services.{SmallGroupService, ProfileService, UserLookupService, SecurityService}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.RelationshipService

class ViewProfileControllerTest extends TestBase with Mockito{

	val controller = new ViewProfileController
	// need to have a security service defined or we'll get a NPE in PermissionsCheckingMethods.restricted()
	controller.securityService = mock[SecurityService]
	controller.smallGroupService = mock[SmallGroupService]
	controller.profileService = mock[ProfileService]

	val member = new StudentMember()
	val courseDetails = new StudentCourseDetails()
	courseDetails.mostSignificant = true
	member.studentCourseDetails.add(courseDetails)

	@Test(expected=classOf[ItemNotFoundException])
	def throwsNonStudent() {
		withUser("test") {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
			val staffMember = new StaffMember()
			val cmd = controller.viewProfileCommand(staffMember)
		}
	}

	@Test
	def getsProfileCommand() {
		withUser("test") {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
			val cmd = controller.viewProfileCommand(member)
			cmd.value should be(member)
		}
	}

	@Test def getMeetingRecordCommand() {
		withUser("test") {
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
			
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)			
			val cmd = controller.getViewMeetingRecordCommand(member, relationshipType)
			cmd should not be(None)

			val staffMember = new StaffMember
			val notACmd = controller.getViewMeetingRecordCommand(staffMember, relationshipType)
			notACmd should be(None)
		}
	}
}