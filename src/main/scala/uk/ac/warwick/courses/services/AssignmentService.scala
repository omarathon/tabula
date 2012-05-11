package uk.ac.warwick.courses.services

import scala.collection.JavaConversions.asScalaBuffer
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order

trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
	def save(assignment:Assignment)
	def save(assignment:UpstreamAssignment)
	def save(group:UpstreamAssessmentGroup)
	def saveSubmission(submission:Submission)
	def getSubmission(assignment:Assignment, userId:String) : Option[Submission]
	def getSubmission(id:String) : Option[Submission]
	
	def delete(submission:Submission) : Unit
	
	def getAssignmentByNameYearModule(name:String, year:AcademicYear, module:Module): Option[Assignment]
	
	def getUsersForFeedback(assignment:Assignment): Seq[Pair[String,User]]
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId:String): Seq[Assignment]
	
	/**
	 * Find a recent assignment within this module. 
	 */
	def recentAssignment(module:Module): Option[Assignment]
	
	def getAssessmentGroup(assignment:Assignment): Option[UpstreamAssessmentGroup]
	
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms {
	import Restrictions._
	
	@Autowired var userLookup:UserLookupService =_
	@Autowired var auditEventIndexService:AuditEventIndexService =_
	
	def getAssignmentById(id:String) = getById[Assignment](id)
	def save(assignment:Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission:Submission) = session.saveOrUpdate(submission)
	
	/**
	 * Tries to find an identical UpstreamAssignment in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment:UpstreamAssignment) : Option[UpstreamAssignment] = session.newCriteria[UpstreamAssignment]
			.add(Restrictions.eq("moduleCode", assignment.moduleCode))
			.add(Restrictions.eq("sequence", assignment.sequence))
			.uniqueResult
			
	def save(assignment:UpstreamAssignment) =
		find(assignment)
			.map { existing =>
				if (existing needsUpdatingFrom assignment)
					session.update(existing.id, assignment) 
			}
			.getOrElse { session.save(assignment) }
	
	def find(group:UpstreamAssessmentGroup) : Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
			.add(Restrictions.eq("academicYear", group.academicYear))
			.add(Restrictions.eq("moduleCode", group.moduleCode))
			.add(Restrictions.eq("occurrence", group.occurrence))
			.uniqueResult
	
	def save(group:UpstreamAssessmentGroup) = 
		find(group)
		.map { existing =>
			// do nothing. nothing else to update except members
			//session.update(existing.id, group)
		}
		.getOrElse{ session.save(group) }
	
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
	
	def recentAssignment(module:Module) = 
		auditEventIndexService.recentAssignment(module)
		
	def getAssessmentGroup(assignment:Assignment): Option[UpstreamAssessmentGroup] = {
		Option(assignment.upstreamAssignment).flatMap { upstream =>
			session.newCriteria[UpstreamAssessmentGroup]
					.add(Restrictions.eq("academicYear", assignment.academicYear))
					.add(Restrictions.eq("moduleCode", upstream.moduleCode ))
					.add(Restrictions.eq("assessmentGroup", upstream.assessmentGroup ))
					.add(Restrictions.eq("occurrence", assignment.occurrence ))
					.uniqueResult
		}
	}
}