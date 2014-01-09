package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.roles.AssignmentSubmitter
import uk.ac.warwick.tabula.services.AssignmentService
import org.mockito.Matchers._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class AssignmentSubmitterRoleProviderTest extends TestBase with Mockito {
	
	val provider = new AssignmentSubmitterRoleProvider
	val assignmentMembershipService = mock[AssignmentMembershipService]
	
	@Test def unrestrictedAssignment = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = Fixtures.module("in101")
		assignment.module.department = Fixtures.department("in")
		
		assignment.restrictSubmissions = false
		
		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}
	
	@Test def canSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = Fixtures.module("in101")
		assignment.module.department = Fixtures.department("in")
		
		assignment.assignmentMembershipService = assignmentMembershipService
		assignment.restrictSubmissions = true
		
		assignmentMembershipService.isStudentMember(
				isEq(currentUser.apparentUser), 
				isA[Seq[UpstreamAssessmentGroup]], 
				isA[Option[UserGroup]]) returns (true)
		
		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}
	
	@Test def cannotSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.assignmentMembershipService = assignmentMembershipService
		assignment.restrictSubmissions = true
		
		assignmentMembershipService.isStudentMember(
				isEq(currentUser.apparentUser), 
				isA[Seq[UpstreamAssessmentGroup]], 
				isA[Option[UserGroup]]) returns (false)
		
		provider.getRolesFor(currentUser, assignment) should be (Seq())
	}
	
	@Test def handlesDefault = withUser("cuscav") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IT Services")) should be (Seq())
	}

}