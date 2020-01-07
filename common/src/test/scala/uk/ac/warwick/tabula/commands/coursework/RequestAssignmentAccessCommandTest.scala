package uk.ac.warwick.tabula.commands.coursework

import freemarker.template.Configuration
import org.mockito.Mockito._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.coursework.RequestAssignmentAccessCommandTest.MinimalCommandContext
import uk.ac.warwick.tabula.commands.coursework.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, UserGroup}
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._


class RequestAssignmentAccessCommandTest extends TestBase with FunctionalContextTesting with Mockito with AssignmentFixture {

  @Test def sendsNotification() {
    inContext[MinimalCommandContext] {
      val cmd = new RequestAssignmentAccessCommand(assignment.module, assignment, new CurrentUser(student, student))
      val admins = cmd.apply()
      val notifications = cmd.emit(admins)
      notifications.size should be(1)
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
    bean() {
      mock[EventListener]
    }
    bean() {
      mock[NotificationService]
    }
    bean() {
      mock[ScheduledNotificationService]
    }
    bean() {
      mock[UserLookupService]
    }
    bean() {
      mock[Configuration]
    }
    bean() {
      mock[Features]
    }
    bean() {
      mock[TriggerService]
    }
  }

}

trait AssignmentFixture extends Mockito {

  val userLookup: UserLookupService = mock[UserLookupService]

  val ownersGroup: UserGroup = UserGroup.ofUsercodes
  ownersGroup.includedUserIds = Set("admin1", "admin2")
  ownersGroup.userLookup = userLookup

  val student: User = newTestUser("student")
  val admin1: User = newTestUser("admin1")
  val admin2: User = newTestUser("admin2")

  userLookup.usersByUserIds(ownersGroup.includedUserIds.toSeq) returns Map("admin1" -> admin1, "admin2" -> admin2)

  val department = Fixtures.department("in")
  val permissionsService: PermissionsService = mock[PermissionsService]
  permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns ownersGroup

  department.permissionsService = permissionsService
  val module = new Module
  module.adminDepartment = department
  val assignment = new Assignment
  assignment.addDefaultFields()
  assignment.module = module
  assignment.module.code = "AA001"
  assignment.module.name = "Really difficult module"


  def newTestUser(id: String): User = {
    val u = new User(id)
    u.setFoundUser(true)
    u.setWarwickId("1000000")
    u.setEmail("test@warwick.ac.uk")
    u
  }

}
