package uk.ac.warwick.tabula.system.permissions

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.Department

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
	
	@Test def linkedSubmissionToAssignment {
		val ass1 = Fixtures.assignment("my assignment")
		ass1.id = "ass1"
			
		val ass2 = Fixtures.assignment("my assignment2")
		ass2.id = "ass2"
		
		val submission = Fixtures.submission()
			
		submission.assignment = ass1
		
		mustBeLinked(submission, ass1)
		
		submission.assignment = ass2
		
		try {
			mustBeLinked(submission, ass1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}
	
	@Test def linkedMarkingWorkflowToDepartment {
		val markingWorkflow = Fixtures.markingWorkflow("my workflow")
		markingWorkflow.department = dept
		
		mustBeLinked(markingWorkflow, dept)
		
		val dept2 = Fixtures.department("xx", "dept 2")
		dept2.id = "dept2"
		
		markingWorkflow.department = dept2
		
		try {
			mustBeLinked(markingWorkflow, dept)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}
	
	@Test def linkedFeedbackTemplateToDepartment {
		val template = Fixtures.feedbackTemplate("my template")
		template.department = dept
		
		mustBeLinked(template, dept)
		
		val dept2 = Fixtures.department("xx", "dept 2")
		dept2.id = "dept2"
		
		template.department = dept2
		
		try {
			mustBeLinked(template, dept)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}
	
	@Test def mandatory {
		val assignment = Fixtures.assignment("my assignment")
		mandatory(assignment) should be (assignment)
		
		try {
			mandatory(null)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
		
		mandatory(Some("yes")) should be ("yes")
		
		try {
			mandatory(None)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}
	
	@Test def notDeleted {
		val assignment = Fixtures.assignment("my assignment")
		notDeleted(assignment)
		
		try {
			notDeleted(assignment)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

}