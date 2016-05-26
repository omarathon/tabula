package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, SecurityService, SmallGroupService, TermService}
import uk.ac.warwick.tabula._
import uk.ac.warwick.userlookup.User

class OldViewProfileControllerTest extends TestBase with Mockito{

	val controller = new OldViewProfileByStudentController
	// need to have a security service defined or we'll get a NPE in PermissionsCheckingMethods.restricted()
	controller.securityService = mock[SecurityService]
	controller.smallGroupService = mock[SmallGroupService]
	controller.profileService = mock[ProfileService]
	controller.termService = mock[TermService]

	val member = new StudentMember()
	val courseDetails = new StudentCourseDetails()
	courseDetails.mostSignificant = true
	member.attachStudentCourseDetails(courseDetails)
	member.mostSignificantCourse = courseDetails
	member.homeDepartment = Fixtures.department("its")

	val user = new CurrentUser(null, new User(){
		setDepartmentCode("its")
	})

	@Test(expected=classOf[ItemNotFoundException])
	def throwsNonStudentStaff() {
		withCurrentUser(user) {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns Nil
			val emeritusMember = new EmeritusMember()
			val cmd = controller.viewProfileCommand(emeritusMember)
		}
	}

	@Test
	def getsProfileCommand() {
		withCurrentUser(user) {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns Nil
			val cmd = controller.viewProfileCommand(member)
			cmd.value should be(member)
		}
	}


}