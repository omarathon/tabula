package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.data.model.UpstreamAssignment

class AssignmentServiceTest extends AppContextTestBase {
	
	@Autowired var service:AssignmentServiceImpl =_

	@Transactional @Test def recentAssignment {
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		session.save(assignment)

		service.recentAssignment(department).get should be (assignment)
	}

	/**
	 * The Hibernate filter that adds deleted != 0
	 */
	@Transactional @Test def notDeletedFilter {
		val module = new Module
		session.save(module)
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignment.markDeleted()
		assignment.addDefaultFields
		session.save(assignment)
		
		assignment.fields.get(1)
		
		service.isFilterEnabled("notDeleted") should be (false)
		service.getAssignmentById(assignment.id) should be (Some(assignment))
		session.enableFilter("notDeleted")
		service.getAssignmentById(assignment.id) should be (None)
		
		service.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) should be (None)
	}
	
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
	
	@Transactional @Test def updateUpstreamAssignment {
		val upstream = new UpstreamAssignment
		upstream.departmentCode = "ch"
		upstream.moduleCode = "ch101"
		upstream.sequence = "A01"
		upstream.assessmentGroup = "A"
		upstream.name = "Egg plants"
		
		service.save(upstream)
		
		val upstream2 = new UpstreamAssignment
        upstream2.departmentCode = "ch"
        upstream2.moduleCode = "ch101"
        upstream2.sequence = "A01"
        upstream2.assessmentGroup = "A"
        upstream2.name = "Greg's plants"
		
        service.save(upstream2)
	}
	
	@Transactional @Test def findModulesWithFeedback {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"
		
		val myFeedback = new Feedback
		myFeedback.universityId = ThisUser
		myFeedback.released = true
		
		val otherFeedback = new Feedback
		otherFeedback.universityId = OtherUser
		otherFeedback.released = true
		
		val unreleasedFeedback = new Feedback
		unreleasedFeedback.universityId = ThisUser
			
		val deletedFeedback = new Feedback
		deletedFeedback.universityId = ThisUser
		deletedFeedback.released = true
		
		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted
		
		assignment1.addFeedback(myFeedback)
		assignment1.addFeedback(otherFeedback)
		assignment2.addFeedback(unreleasedFeedback)
		assignment3.addFeedback(deletedFeedback)
		
		session.save(assignment1)
		session.save(assignment2)
		session.save(assignment3)
		
		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		session.save(deletedFeedback)
		
		session.enableFilter("notDeleted")
		
		val assignments = service.getAssignmentsWithFeedback(ThisUser)
		assignments.size should be (1)
		assignments(0) should be (assignment1)
	}
}