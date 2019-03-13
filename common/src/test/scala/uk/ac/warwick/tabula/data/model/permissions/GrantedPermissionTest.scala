package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}

class GrantedPermissionTest extends TestBase {

	val dept: Department = Fixtures.department("in")
	val module: Module = Fixtures.module("in101")
	val assignment: Assignment = Fixtures.assignment("assignment")
	val member: StaffMember = Fixtures.staff()

	val feedback: AssignmentFeedback = Fixtures.assignmentFeedback()

	val permission = Permissions.Module.Create
	val overrideType = true

	@Test def initDepartment {
		GrantedPermission.canDefineFor[Department] should be (true)
		val gp = GrantedPermission(dept, permission, overrideType)
		gp.scope should be (dept)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}

	@Test def initModule {
		GrantedPermission.canDefineFor[Module] should be (true)
		val gp = GrantedPermission(module, permission, overrideType)
		gp.scope should be (module)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}

	@Test def initAssignment {
		GrantedPermission.canDefineFor[Assignment] should be (true)
		val gp = GrantedPermission(assignment, permission, overrideType)
		gp.scope should be (assignment)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}

	@Test def initMember {
		GrantedPermission.canDefineFor[Member] should be (true)
		val gp = GrantedPermission(member, permission, overrideType)
		gp.scope should be (member)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}

	@Test(expected = classOf[IllegalArgumentException]) def initInvalid {
		GrantedPermission.canDefineFor[AssignmentFeedback] should be (false)
		GrantedPermission(feedback, permission, overrideType)
	}

	@Test def scopeType(): Unit = {
		GrantedPermission.scopeType[Assignment] should be (Some("Assignment"))
		GrantedPermission.scopeType[PermissionsTarget] should be (None)
	}

}