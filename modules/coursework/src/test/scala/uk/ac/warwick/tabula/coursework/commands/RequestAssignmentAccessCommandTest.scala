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
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.commands.RequestAssignmentAccessCommandTest.MinimalCommandContext


class RequestAssignmentAccessCommandTest extends TestBase with FunctionalContextTesting with Mockito with AssignmentFixture {

	@Test def sendsNotification() {
		inContext[MinimalCommandContext] {
			val cmd = new RequestAssignmentAccessCommand(assignment.module, assignment, new CurrentUser(student, student))
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

	val ownersGroup = UserGroup.ofUsercodes
	ownersGroup.includeUsers = List("admin1", "admin2").asJava
	ownersGroup.userLookup = userLookup

	val student = newTestUser("student")
	val admin1 = newTestUser("admin1")
	val admin2 = newTestUser("admin2")
	
	userLookup.getUsersByUserIds(ownersGroup.includeUsers) returns JMap("admin1" -> admin1, "admin2" -> admin2)
	
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