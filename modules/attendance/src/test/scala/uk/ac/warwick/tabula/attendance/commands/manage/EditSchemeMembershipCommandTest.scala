package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent, UserLookupComponent, ProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.permissions.Permissions

class EditSchemeMembershipCommandTest extends TestBase with Mockito {

	@Test
	def addUsers() { withUser("cusfal") {
		val command = new AddsUsersToEditSchemeMembershipCommand with ProfileServiceComponent with UserLookupComponent
			with SecurityServiceComponent with EditSchemeMembershipCommandState {

			val profileService = smartMock[ProfileService]
			val userLookup = new MockUserLookup
			val securityService = smartMock[SecurityService]

			val scheme = null
			val user = currentUser
		}

		val validStudent = Fixtures.student("1111111","abcd")
		val validStudentWithUsercode = Fixtures.student("2222222","cdef")
		val invalidStaff = Fixtures.staff("3333333","abcd")
		val invalidNoone = "unknown"
		val invalidNoPermission = Fixtures.student("4444444")

		command.massAddUsers = "%s\n%s\n%s\n%s\n%s" format (
			validStudent.universityId,
			validStudentWithUsercode.userId,
			invalidStaff.universityId,
			invalidNoone,
			invalidNoPermission.universityId
		)

		command.profileService.getMemberByUniversityId(validStudent.universityId) returns Option(validStudent)
		command.profileService.getMemberByUniversityId(invalidStaff.universityId) returns Option(invalidStaff)
		command.profileService.getMemberByUniversityId(invalidNoPermission.universityId) returns Option(invalidNoPermission)
		command.userLookup.registerUserObjects(MemberOrUser(validStudentWithUsercode).asUser)
		command.profileService.getMemberByUser(MemberOrUser(validStudentWithUsercode).asUser) returns Option(validStudentWithUsercode)

		command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, validStudent) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, validStudentWithUsercode) returns true
		command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, invalidNoPermission) returns false

		val result = command.addUsers()
		result.missingMembers.size should be (2)
		result.missingMembers.contains(invalidNoone) should be (true)
		result.missingMembers.contains(invalidStaff.universityId) should be (true)
		result.noPermissionMembers.size should be (1)
		result.noPermissionMembers.head should be (invalidNoPermission)
		command.updatedIncludedStudentIds.size should be (2)
		command.updatedIncludedStudentIds.contains(validStudent.universityId) should be (true)
		command.updatedIncludedStudentIds.contains(validStudentWithUsercode.universityId) should be (true)
	}}

}
