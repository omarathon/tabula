package uk.ac.warwick.courses.services

import scala.collection.JavaConversions.asScalaBuffer
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.Restrictions

trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
	def save(assignment:Assignment)
	def saveSubmission(submission:Submission)
	def getSubmission(assignment:Assignment, userId:String) : Option[Submission]
	def getSubmission(id:String) : Option[Submission]
	
	def delete(submission:Submission) : Unit
	
	def getAssignmentByNameYearModule(name:String, year:AcademicYear, module:Module): Option[Assignment]
	
	def getUsersForFeedback(assignment:Assignment): Seq[Pair[String,User]]
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId:String): Seq[Assignment]
	
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms {
	
	@Autowired var userLookup:UserLookupService =_
	
	def getAssignmentById(id:String) = getById[Assignment](id)
	def save(assignment:Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission:Submission) = {
//		for (value <- submission.values) session.saveOrUpdate(value)
		session.saveOrUpdate(submission)
	}
	
	def getSubmission(assignment:Assignment, userId:String) = {
		session.newCriteria[Submission]
				.add(Restrictions.eq("assignment", assignment))
				.add(Restrictions.eq("userId", userId))
				.uniqueResult
	}
	
	def getSubmission(id:String) = getById[Submission](id)
	
	def delete(submission:Submission) = {
		submission.assignment.submissions.remove(submission)
		session.delete(submission)
	}
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment] =
		session.createQuery("""select distinct a from Assignment a
				join a.feedbacks as f
				where f.universityId = :universityId
				and f.released=true""")
			.setString("universityId", universityId)
			.list.asInstanceOf[JList[Assignment]]
	
	def getAssignmentsWithSubmission(universityId:String): Seq[Assignment] =
		session.createQuery("""select distinct a from Assignment a
				join a.submissions as f
				where f.universityId = :universityId""")
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