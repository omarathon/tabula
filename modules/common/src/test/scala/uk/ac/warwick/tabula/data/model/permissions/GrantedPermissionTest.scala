package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{TestBase, Fixtures}
import uk.ac.warwick.tabula.permissions.Permissions

class GrantedPermissionTest extends TestBase {
	
	val dept = Fixtures.department("in")
	val module = Fixtures.module("in101")
	val assignment = Fixtures.assignment("assignment")
	val member = Fixtures.staff()
	
	val feedback = Fixtures.feedback()
	
	val permission = Permissions.Module.Create
	val overrideType = true
	
	@Test def initDepartment {
		GrantedPermission.canDefineFor(dept) should be (true)
		val gp = GrantedPermission(dept, permission, overrideType)
		gp.scope should be (dept)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}
	
	@Test def initModule {
		GrantedPermission.canDefineFor(module) should be (true)
		val gp = GrantedPermission(module, permission, overrideType)
		gp.scope should be (module)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}
	
	@Test def initAssignment {
		GrantedPermission.canDefineFor(assignment) should be (true)
		val gp = GrantedPermission(assignment, permission, overrideType)
		gp.scope should be (assignment)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}
	
	@Test def initMember {
		GrantedPermission.canDefineFor(member) should be (true)
		val gp = GrantedPermission(member, permission, overrideType)
		gp.scope should be (member)
		gp.permission should be (permission)
		gp.overrideType should be (overrideType)
	}
	
	@Test(expected = classOf[IllegalArgumentException]) def initInvalid {
		GrantedPermission.canDefineFor(feedback) should be (false)
		GrantedPermission(feedback, permission, overrideType)
	}

}