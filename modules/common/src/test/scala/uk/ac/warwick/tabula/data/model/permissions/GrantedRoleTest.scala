package uk.ac.warwick.tabula.data.model.permissions

import uk.ac.warwick.tabula.{TestBase, Fixtures}
import uk.ac.warwick.tabula.roles.AssignmentSubmitterRoleDefinition

class GrantedRoleTest extends TestBase {
	
	val dept = Fixtures.department("in")
	val module = Fixtures.module("in101")
	val assignment = Fixtures.assignment("assignment")
	val member = Fixtures.staff()
	
	val feedback = Fixtures.feedback()
	
	val roleDefinition = AssignmentSubmitterRoleDefinition
	
	@Test def initDepartment {
		GrantedRole.canDefineFor(dept) should be (true)
		val gr = GrantedRole.init(dept, roleDefinition)
		gr.scope should be (dept)
		gr.roleDefinition should be (roleDefinition)
	}
	
	@Test def initModule {
		GrantedRole.canDefineFor(module) should be (true)
		val gr = GrantedRole.init(module, roleDefinition)
		gr.scope should be (module)
		gr.roleDefinition should be (roleDefinition)
	}
	
	@Test def initAssignment {
		GrantedRole.canDefineFor(assignment) should be (true)
		val gr = GrantedRole.init(assignment, roleDefinition)
		gr.scope should be (assignment)
		gr.roleDefinition should be (roleDefinition)
	}
	
	@Test def initMember {
		GrantedRole.canDefineFor(member) should be (true)
		val gr = GrantedRole.init(member, roleDefinition)
		gr.scope should be (member)
		gr.roleDefinition should be (roleDefinition)
	}
	
	@Test(expected = classOf[IllegalArgumentException]) def initInvalid {
		GrantedRole.canDefineFor(feedback) should be (false)
		GrantedRole.init(feedback, roleDefinition)
	}

}