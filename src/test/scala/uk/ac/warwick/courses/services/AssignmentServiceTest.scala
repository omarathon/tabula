package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Autowired

class AssignmentServiceTest extends AppContextTestBase {
	
	@Autowired var service:AssignmentService =_
	
	@Transactional @Test def findDuplicateAssignmentNames {
		val module = new Module
		session.save(module)
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('empty)
		
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		session.save(assignment)
		session.flush()
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('defined)
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2008), module) should be ('empty)
		service.getAssignmentByNameYearModule("Blessay", new AcademicYear(2009), module) should be ('empty)
	}
}