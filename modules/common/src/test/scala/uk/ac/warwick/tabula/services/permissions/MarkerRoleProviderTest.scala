package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase, Fixtures}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.roles.Marker
import uk.ac.warwick.tabula.helpers.Promises._

class MarkerRoleProviderTest extends TestBase with Mockito {

	val mockAssignmentService = smartMock[AssignmentService]
	val provider = new MarkerRoleProvider {
		override val assignmentService = promise { mockAssignmentService }
	}
	
	val mw1 = Fixtures.seenSecondMarkingLegacyWorkflow("workflow is marker")
	mw1.firstMarkers.knownType.addUserId("cuscav")
	
	val mw2 = Fixtures.seenSecondMarkingLegacyWorkflow("workflow not marker")
	mw2.firstMarkers.knownType.addUserId("cusebr")
	
	val assignmentIsMarker1 = Fixtures.assignment("assignment is marker 1")	
	assignmentIsMarker1.markingWorkflow = mw1
	
	val assignmentIsMarker2 = Fixtures.assignment("assignment is marker 2")
	assignmentIsMarker2.markingWorkflow = mw1
	
	val assignmentNotMarker = Fixtures.assignment("not marker")
	assignmentNotMarker.markingWorkflow = mw2
	
	val mod1 = Fixtures.module("mod1", "mod 1")
	val mod2 = Fixtures.module("mod2", "mod 2")
	val mod3 = Fixtures.module("mod3", "mod 3")
	
	val dept = Fixtures.department("dept", "department")
	
	mod1.adminDepartment = dept
	mod2.adminDepartment = dept
	mod3.adminDepartment = dept
	
	assignmentIsMarker1.module = mod1
	assignmentIsMarker2.module = mod2
	assignmentNotMarker.module = mod3
	
	dept.modules.addAll(Seq(mod1, mod2, mod3))
	mod1.assignments.addAll(Seq(assignmentIsMarker1, assignmentNotMarker))
	mod2.assignments.addAll(Seq(assignmentIsMarker2))
	mod3.assignments.addAll(Seq(assignmentNotMarker))

	val cuscavUser = new CurrentUser(new User("cuscav"), new User("cuscav"))

	mockAssignmentService.getAssignmentsByDepartmentAndMarker(dept, cuscavUser) returns Seq(assignmentIsMarker1, assignmentIsMarker2)
	mockAssignmentService.getAssignmentsByModuleAndMarker(mod1, cuscavUser) returns Seq(assignmentIsMarker1)
	mockAssignmentService.getAssignmentsByModuleAndMarker(mod2, cuscavUser) returns Seq(assignmentIsMarker2)
	mockAssignmentService.getAssignmentsByModuleAndMarker(mod3, cuscavUser) returns Seq()
	
	@Test def forAssignment() = withCurrentUser(cuscavUser) {
		provider.getRolesFor(currentUser, assignmentIsMarker1) should be (Seq(Marker(assignmentIsMarker1)))
		provider.getRolesFor(currentUser, assignmentIsMarker2) should be (Seq(Marker(assignmentIsMarker2)))
		provider.getRolesFor(currentUser, assignmentNotMarker) should be (Seq())
	}
	
	@Test def forModule() = withCurrentUser(cuscavUser) {
		provider.getRolesFor(currentUser, mod1) should be (Seq(Marker(assignmentIsMarker1)))
		provider.getRolesFor(currentUser, mod2) should be (Seq(Marker(assignmentIsMarker2)))
		provider.getRolesFor(currentUser, mod3) should be (Seq())
	}
	
	@Test def forDepartment() = withCurrentUser(cuscavUser) {
		provider.getRolesFor(currentUser, dept) should be (Seq(Marker(assignmentIsMarker1), Marker(assignmentIsMarker2)))
	}
	
	@Test def handlesDefault() = withCurrentUser(cuscavUser) {
		provider.getRolesFor(currentUser, Fixtures.feedback()) should be (Seq())
	}

}