package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Feedback

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
	
	@Transactional @Test def findModulesWithFeedback {
		val myFeedback = new Feedback
		myFeedback.universityId = "1234567"
		myFeedback.released = true
		
		val otherFeedback = new Feedback
		otherFeedback.universityId = "1234568"
		otherFeedback.released = true
		
		val unreleasedFeedback = new Feedback
		unreleasedFeedback.universityId = "1234568"
		
		val assignment1 = new Assignment
		val assignment2 = new Assignment
		
		assignment1.feedbacks.add(myFeedback)
		myFeedback.assignment = assignment1
		
		assignment1.feedbacks.add(otherFeedback)
		otherFeedback.assignment = assignment1
		
		assignment2.feedbacks.add(unreleasedFeedback)
		unreleasedFeedback.assignment = assignment2
		
		session.save(assignment1)
		session.save(assignment2)
		
		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		
		val assignments = service.getAssignmentsWithFeedback("1234567")
		assignments.size should be (1)
		assignments(0) should be (assignment1)
	}
}