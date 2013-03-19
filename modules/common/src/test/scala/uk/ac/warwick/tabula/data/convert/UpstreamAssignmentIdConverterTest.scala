package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.UpstreamAssignment
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.Mockito

class UpstreamAssignmentIdConverterTest extends TestBase with Mockito {
	
	val converter = new UpstreamAssignmentIdConverter
	val service = mock[AssignmentMembershipService]
	converter.service = service
	
	@Test def validInput {
		val assignment = new UpstreamAssignment
		assignment.id = "steve"
			
		service.getUpstreamAssignment("steve") returns (Some(assignment))
		
		converter.convertRight("steve") should be (assignment)
	}
	
	@Test def invalidInput {
		service.getUpstreamAssignment("20X6") returns (None)
		
		converter.convertRight("20X6") should be (null)
	}
	
	@Test def formatting {
		val assignment = new UpstreamAssignment
		assignment.id = "steve"
		
		converter.convertLeft(assignment) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}