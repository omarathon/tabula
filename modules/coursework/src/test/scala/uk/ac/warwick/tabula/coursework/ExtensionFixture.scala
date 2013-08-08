package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{MockUserLookup, Mockito}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import collection.JavaConversions._

trait ExtensionFixture extends Mockito{

	val studentMember = new StudentMember
	studentMember.universityId = "student"
	val student = studentMember.asSsoUser

	val adminMember = new StaffMember
	adminMember.universityId = "admin"
	adminMember.userId = "admin"
	val admin = adminMember.asSsoUser

	val adminMember2 = new StaffMember
	adminMember2.universityId = "admin2"
	adminMember2.userId = "admin2"
	val admin2 = adminMember2.asSsoUser

	val adminMember3 = new StaffMember
	adminMember3.universityId = "admin3"
	adminMember3.userId = "admin3"
	val admin3 = adminMember3.asSsoUser

	val otherAdmins = Seq(admin2, admin3)

	val userLookup = new MockUserLookup
	userLookup.users = Map("admin" -> admin, "admin2" -> admin2, "admin3" -> admin3)
	val extensionManagers = UserGroup.ofUsercodes
	extensionManagers.userLookup = userLookup
	extensionManagers.includeUsers.add("admin")
	extensionManagers.includeUsers.add("admin2")
	extensionManagers.includeUsers.add("admin3")

	val department = new Department
	val permissionsService = mock[PermissionsService]
	when(permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition)) thenReturn extensionManagers
	department.permissionsService = permissionsService

	val module = new Module
	module.department = department
	module.code = "xxx"
	val assignment = new Assignment
	assignment.id = "123"
	assignment.closeDate = new DateTime(2013, 8, 1, 12, 0)
	assignment.module = module

	val extension = new Extension(student.getWarwickId)
	extension.expiryDate = new DateTime(2013, 8, 23, 12, 0)
	extension.requestedExpiryDate = new DateTime(2013, 8, 23, 12, 0)
	extension.reason = "My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me."
	extension.approvalComments = "That sounds awful. Have an extra month. By then you should be able to write as well as any Cetacea."
	extension.assignment = assignment
	assignment.extensions add extension
}
