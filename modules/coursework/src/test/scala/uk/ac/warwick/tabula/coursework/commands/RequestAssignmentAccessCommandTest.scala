package uk.ac.warwick.tabula.coursework.commands

import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{UserGroup, Department, Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula.{CurrentUser, Mockito, AppContextTestBase}
import uk.ac.warwick.tabula.services.UserLookupService
import org.mockito.Mockito._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition


class RequestAssignmentAccessCommandTest extends AppContextTestBase with Mockito with AssignmentFixture {

	@Test def sendsNotification{
		val cmd = new RequestAssignmentAccessCommand(new CurrentUser(student, student))
		cmd.userLookup = userLookup
		cmd.assignment = assignment
		cmd.module = assignment.module
		cmd.apply()
		val notifications = cmd.emit
		notifications.size should be (1)
	}
}


trait AssignmentFixture extends Mockito{

	val userLookup = mock[UserLookupService]
	when(userLookup.getUserByUserId("admin1")).thenReturn(admin1)
	when(userLookup.getUserByUserId("admin2")).thenReturn(admin2)

	val ownersGroup = new UserGroup
	ownersGroup.includeUsers = List("admin1", "admin2").asJava

	val student = newTestUser("student")
	val admin1 = newTestUser("admin1")
	val admin2 = newTestUser("admin2")

	val department = new Department
	val permissionsService = mock[PermissionsService]
	permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns ownersGroup

	department.permissionsService = permissionsService
	val module = new Module
	module.department = department
	val assignment = new Assignment
	assignment.addDefaultFields
	assignment.module = module
	assignment.module.code = "AA001"
	assignment.module.name = "Really difficult module"


	def newTestUser(id: String) = {
		val u = new User(id)
		u.setFoundUser(true)
		u.setWarwickId("1000000")
		u.setEmail("test@warwick.ac.uk")
		u
	}

}
