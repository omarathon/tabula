package uk.ac.warwick.tabula.coursework.commands

import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{UserGroup, Department, Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.{NotificationService, MaintenanceModeService, UserLookupService}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import freemarker.template.Configuration
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.coursework.commands.RequestAssignmentAccessCommandTest.MinimalCommandContext


class RequestAssignmentAccessCommandTest extends TestBase with FunctionalContextTesting with Mockito with AssignmentFixture {

	@Test def sendsNotification() {
		inContext[MinimalCommandContext] {
			val cmd = new RequestAssignmentAccessCommand(new CurrentUser(student, student))
			cmd.userLookup = userLookup
			cmd.assignment = assignment
			cmd.module = assignment.module
			val admins = cmd.apply()
			val notifications = cmd.emit(admins)
			notifications.size should be (1)
		}
	}
}
object RequestAssignmentAccessCommandTest {

	class MinimalCommandContext extends FunctionalContext with Mockito {
		bean() {
			val maintenanceMode = mock[MaintenanceModeService]
			when(maintenanceMode.enabled).thenReturn(false)
			maintenanceMode
		}
		bean(){mock[EventListener]}
		bean(){mock[NotificationService]}
		bean(){mock[UserLookupService]}
		bean(){mock[Configuration]}
	}
}

trait AssignmentFixture extends Mockito{

	val userLookup = mock[UserLookupService]
	when(userLookup.getUserByUserId("admin1")).thenReturn(admin1)
	when(userLookup.getUserByUserId("admin2")).thenReturn(admin2)

	val ownersGroup = UserGroup.ofUsercodes
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
	assignment.addDefaultFields()
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