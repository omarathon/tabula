package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{Department, Notification, UserGroup}
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula._
import uk.ac.warwick.userlookup.User

class ManualMembershipWarningNotificationTest extends TestBase with Mockito with FreemarkerRendering with FreemarkerTestHelpers {

	sealed trait Fixture {
		val user: User = Fixtures.user(universityId = "1234657", userId = "u1234657")
		val department: Department = Fixtures.department(code = "in", name = "IT Services")
		department.id = "departmentId"

		val userLookup: MockUserLookup = new MockUserLookup()

		val owner1: User = Fixtures.user("0000001", "u0000001")
		val owner2: User = Fixtures.user("0000002", "u0000002")
		userLookup.registerUserObjects(user, owner1, owner2)

		val ownersGroup: UserGroup = Fixtures.userGroup(owner1, owner2)
		ownersGroup.userLookup = userLookup

		department.permissionsService = smartMock[PermissionsService]
		department.permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns ownersGroup

		val notification: ManualMembershipWarningNotification = Notification.init(new ManualMembershipWarningNotification, user, department)
		notification.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]

		notification.moduleAndDepartmentService.getDepartmentById("departmentId") returns Some(department)

		implicit val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
	}

	sealed trait SingleAssignmentFixture extends Fixture {
		notification.numAssignments = 1
	}

	sealed trait SingleSmallGroupSetFixture extends Fixture {
		notification.numSmallGroupSets = 1
	}

	sealed trait MultipleAssignmentsFixture extends Fixture {
		notification.numAssignments = 3
	}

	sealed trait MultipleSmallGroupSetsFixture extends Fixture {
		notification.numSmallGroupSets = 3
	}

	@Test
	def recipients(): Unit = {
		new SingleAssignmentFixture {
			notification.recipients should be (Seq(owner1, owner2))
		}
	}

	@Test
	def title(): Unit = {
		new SingleAssignmentFixture {
			notification.title should be ("1 assignment in IT Services has manually added students")
		}

		new SingleSmallGroupSetFixture {
			notification.title should be ("1 small group set in IT Services has manually added students")
		}

		new SingleAssignmentFixture with SingleSmallGroupSetFixture {
			notification.title should be ("1 assignment and 1 small group set in IT Services have manually added students")
		}

		new MultipleAssignmentsFixture {
			notification.title should be ("3 assignments in IT Services have manually added students")
		}

		new MultipleSmallGroupSetsFixture {
			notification.title should be ("3 small group sets in IT Services have manually added students")
		}

		new MultipleAssignmentsFixture with MultipleSmallGroupSetsFixture {
			notification.title should be ("3 assignments and 3 small group sets in IT Services have manually added students")
		}

		new SingleAssignmentFixture with MultipleSmallGroupSetsFixture {
			notification.title should be ("1 assignment and 3 small group sets in IT Services have manually added students")
		}

		new MultipleAssignmentsFixture with SingleSmallGroupSetFixture {
			notification.title should be ("3 assignments and 1 small group set in IT Services have manually added students")
		}
	}

	@Test
	def content(): Unit = {
		new SingleAssignmentFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 1 assignment in IT Services:")
			content should include ("Update the membership of the assignment in SITS")
			content should include ("remove any manually added students from the assignment in Tabula")
		}

		new SingleSmallGroupSetFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 1 small group set in IT Services:")
			content should include ("Update the membership of the small group set in SITS")
			content should include ("remove any manually added students from the small group set in Tabula")
		}

		new SingleAssignmentFixture with SingleSmallGroupSetFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 1 assignment and 1 small group set in IT Services:")
			content should include ("Update the membership of the assignment and small group set in SITS")
			content should include ("remove any manually added students from the assignment and small group set in Tabula")
		}

		new MultipleAssignmentsFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 3 assignments in IT Services:")
			content should include ("Update the membership of the assignments in SITS")
			content should include ("remove any manually added students from the assignments in Tabula")
		}

		new MultipleSmallGroupSetsFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 3 small group sets in IT Services:")
			content should include ("Update the membership of the small group sets in SITS")
			content should include ("remove any manually added students from the small group sets in Tabula")
		}

		new MultipleAssignmentsFixture with MultipleSmallGroupSetsFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 3 assignments and 3 small group sets in IT Services:")
			content should include ("Update the membership of the assignments and small group sets in SITS")
			content should include ("remove any manually added students from the assignments and small group sets in Tabula")
		}

		new SingleAssignmentFixture with MultipleSmallGroupSetsFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 1 assignment and 3 small group sets in IT Services:")
			content should include ("Update the membership of the assignment and small group sets in SITS")
			content should include ("remove any manually added students from the assignment and small group sets in Tabula")
		}

		new MultipleAssignmentsFixture with SingleSmallGroupSetFixture {
			val content: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			content should startWith ("There are manually added students in 3 assignments and 1 small group set in IT Services:")
			content should include ("Update the membership of the assignments and small group set in SITS")
			content should include ("remove any manually added students from the assignments and small group set in Tabula")
		}
	}

}
