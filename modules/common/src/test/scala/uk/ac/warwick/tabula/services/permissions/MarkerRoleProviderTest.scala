package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.roles.Marker

class MarkerRoleProviderTest extends TestBase {
	
	val provider = new MarkerRoleProvider
	
	val mw1 = Fixtures.markingWorkflow("workflow is marker")
	mw1.firstMarkers.addUser("cuscav")
	
	val mw2 = Fixtures.markingWorkflow("workflow not marker")
	mw2.firstMarkers.addUser("cusebr")
	
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
	
	dept.modules.addAll(Seq(mod1, mod2, mod3))
	mod1.assignments.addAll(Seq(assignmentIsMarker1, assignmentNotMarker))
	mod2.assignments.addAll(Seq(assignmentIsMarker2))
	mod3.assignments.addAll(Seq(assignmentNotMarker))
	
	@Test def forAssignment = withUser("cuscav") {
		provider.getRolesFor(currentUser, assignmentIsMarker1) should be (Seq(Marker(assignmentIsMarker1)))
		provider.getRolesFor(currentUser, assignmentIsMarker2) should be (Seq(Marker(assignmentIsMarker2)))
		provider.getRolesFor(currentUser, assignmentNotMarker) should be (Seq())
	}
	
	@Test def forModule = withUser("cuscav") {
		provider.getRolesFor(currentUser, mod1) should be (Seq(Marker(assignmentIsMarker1)))
		provider.getRolesFor(currentUser, mod2) should be (Seq(Marker(assignmentIsMarker2)))
		provider.getRolesFor(currentUser, mod3) should be (Seq())
	}
	
	@Test def forDepartment = withUser("cuscav") {
		provider.getRolesFor(currentUser, dept) should be (Seq(Marker(assignmentIsMarker1), Marker(assignmentIsMarker2)))
	}
	
	@Test def handlesDefault = withUser("cuscav") {
		provider.getRolesFor(currentUser, Fixtures.feedback()) should be (Seq())
	}

}