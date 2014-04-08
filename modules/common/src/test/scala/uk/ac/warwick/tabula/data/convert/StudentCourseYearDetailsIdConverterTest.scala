package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails

class StudentCourseYearDetailsIdConverterTest extends TestBase with Mockito {
	
	val converter = new StudentCourseYearDetailsIdConverter

	@Test def validInput {
		val scyd = new StudentCourseYearDetails
		scyd.id = "foo"
			
		converter.convertRight("foo") should be (scyd)
	}
	
	@Test def invalidInput {

		converter.convertRight("bar") should be (null)
	}
	
	@Test def formatting {
		val scyd = new StudentCourseYearDetails
		scyd.id = "foo"
		
		converter.convertLeft(scyd) should be ("foo")
		converter.convertLeft(null) should be (null)
	}

}