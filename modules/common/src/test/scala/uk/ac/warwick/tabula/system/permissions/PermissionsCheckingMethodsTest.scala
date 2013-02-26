package uk.ac.warwick.tabula.system.permissions

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.ItemNotFoundException

class PermissionsCheckingMethodsTest extends TestBase with PermissionsChecking {
	
	val dept = Fixtures.department("in", "IT Services")
	dept.id = "dept"
	
	val mod1 = Fixtures.module("in101", "IN 1")
	mod1.id = "mod1"
	
	val mod2 = Fixtures.module("in102", "IN 2")
	mod2.id = "mod2"
	
	@Test def checks {
		PermissionCheck(Permissions.Module.Create, dept)
		PermissionCheck(Permissions.Module.Delete, dept)
		PermissionCheckAll(Permissions.Module.Read, Seq(mod1, mod2))
		PermissionCheck(Permissions.UserPicker)
		
		permissionChecks should be (Map(
			Permissions.Module.Create -> Some(dept),
			Permissions.Module.Delete -> Some(dept),
			Permissions.Module.Read -> Some(mod1),
			Permissions.Module.Read -> Some(mod2),
			Permissions.UserPicker -> None
		))
	}
	
	@Test def linkedAssignmentToModule {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = mod1
		
		mustBeLinked(assignment, mod1)
		
		assignment.module = mod2
		
		try {
			mustBeLinked(assignment, mod1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}
	
	@Test def linkedFeedbackToAssignment {
		val ass1 = Fixtures.assignment("my assignment")
		ass1.id = "ass1"
			
		val ass2 = Fixtures.assignment("my assignment2")
		ass2.id = "ass2"
		
		val feedback = Fixtures.feedback()
			
		feedback.assignment = ass1
		
		mustBeLinked(feedback, ass1)
		
		feedback.assignment = ass2
		
		try {
			mustBeLinked(feedback, ass1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

}