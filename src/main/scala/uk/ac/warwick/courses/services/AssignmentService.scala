package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.userlookup.User
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired

trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
	def save(assignment:Assignment)
	def saveSubmission(submission:Submission)
	
	def getAssignmentByNameYearModule(name:String, year:AcademicYear, module:Module): Option[Assignment]
	
	def getUsersForFeedback(assignment:Assignment): Seq[Pair[String,User]]
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment]
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms {
	
	@Autowired var userLookup:UserLookupService =_
	
	def getAssignmentById(id:String) = getById[Assignment](id)
	def save(assignment:Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission:Submission) = {
		session.saveOrUpdate(submission)
	}
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment] =
		session.createQuery("""select distinct a from Feedback f
				join f.assignment as a
				where f.universityId = :universityId
				and (f.released=true or a.resultsPublished=true)""")
			.setString("universityId", universityId)
			.list.asInstanceOf[JList[Assignment]]
	
	def getAssignmentByNameYearModule(name:String, year:AcademicYear, module:Module) = {
		option[Assignment](session.createQuery("from Assignment where name=:name and academicYear=:year and module=:module")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.uniqueResult
			)
	}
	
	def getUsersForFeedback(assignment:Assignment): Seq[Pair[String,User]] = {
		val uniIds = assignment.unreleasedFeedback.map { _.universityId }
		uniIds.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }
	}
}