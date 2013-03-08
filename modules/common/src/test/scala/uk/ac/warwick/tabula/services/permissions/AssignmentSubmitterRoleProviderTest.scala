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

class AssignmentSubmitterRoleProviderTest extends TestBase with Mockito {
	
	val provider = new AssignmentSubmitterRoleProvider
	val assignmentService = mock[AssignmentService]
	
	@Test def unrestrictedAssignment = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.restrictSubmissions = false
		
		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}
	
	@Test def canSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.assignmentService = assignmentService
		assignment.restrictSubmissions = true
		
		assignmentService.isStudentMember(
				isEq(currentUser.apparentUser), 
				isA[Seq[UpstreamAssessmentGroup]], 
				isA[Option[UserGroup]]) returns (true)
		
		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}
	
	@Test def cannotSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.assignmentService = assignmentService
		assignment.restrictSubmissions = true
		
		assignmentService.isStudentMember(
				isEq(currentUser.apparentUser), 
				isA[Seq[UpstreamAssessmentGroup]], 
				isA[Option[UserGroup]]) returns (false)
		
		provider.getRolesFor(currentUser, assignment) should be (Seq())
	}
	
	@Test def handlesDefault = withUser("cuscav") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IT Services")) should be (Seq())
	}

}