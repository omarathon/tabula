package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.model.Submission


trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
	def save(assignment:Assignment)
	def saveSubmission(submission:Submission)
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms {
	def getAssignmentById(id:String) = getById[Assignment](id)
	def save(assignment:Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission:Submission) = {
		session.saveOrUpdate(submission)
	}
}