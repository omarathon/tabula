package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.stereotype.Service


trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms {
	def getAssignmentById(id:String) = getById[Assignment](id)
}