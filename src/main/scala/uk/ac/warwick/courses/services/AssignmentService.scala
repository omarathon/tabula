package uk.ac.warwick.courses.services

import scala.collection.JavaConversions._
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
import uk.ac.warwick.courses.helpers.Logging

trait AssignmentService {
	def getAssignmentById(id:String): Option[Assignment]
	def save(assignment:Assignment)
	
	def saveSubmission(submission:Submission)
	def getSubmissionByUniId(assignment:Assignment, uniId:String) : Option[Submission]
	def getSubmission(id:String) : Option[Submission]
	
	def delete(submission:Submission) : Unit
	def deleteOriginalityReport(submission:Submission): Unit
	
	def getAssignmentByNameYearModule(name:String, year:AcademicYear, module:Module): Option[Assignment]
	
	def getUsersForFeedback(assignment:Assignment): Seq[Pair[String,User]]
	
	def getAssignmentsWithFeedback(universityId:String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId:String): Seq[Assignment]
	
	/**
	 * Find a recent assignment within this module. 
	 */
	def recentAssignment(module:Module): Option[Assignment]
	
	def getAssessmentGroup(assignment:Assignment): Option[UpstreamAssessmentGroup]
	def getAssessmentGroup(template:UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]
	
	def getUpstreamAssignment(id:String): Option[UpstreamAssignment]
	
	/** Get all UpstreamAssignments that appear to belong to this module.
	 *  
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getAssessmentGroups.
	 */
	def getUpstreamAssignments(module:Module): Seq[UpstreamAssignment]

	/** Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getAssessmentGroups(upstreamAssignment:UpstreamAssignment, academicYear:AcademicYear): Seq[UpstreamAssessmentGroup]
	
	def save(assignment:UpstreamAssignment)
    def save(group:UpstreamAssessmentGroup)
    def replaceMembers(group:UpstreamAssessmentGroup, universityIds:Seq[String])
}

@Service
class AssignmentServiceImpl extends AssignmentService with Daoisms with Logging {
	import Restrictions._
	
	@Autowired var userLookup:UserLookupService =_
	@Autowired var auditEventIndexService:AuditEventIndexService =_
	
	def getAssignmentById(id:String) = getById[Assignment](id)
	def save(assignment:Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission:Submission) = {
	    if (submission.originalityReport != null && submission.originalityReport.id == null) {
	    	session.save(submission.originalityReport)
	    }
	    session.saveOrUpdate(submission)
	}
	
	def replaceMembers(template:UpstreamAssessmentGroup, universityIds:Seq[String]) {
		if (debugEnabled) debugReplace(template, universityIds)
		getAssessmentGroup(template).map { group =>
			val collection = group.members.staticIncludeUsers
			collection.clear
			collection.addAll(universityIds)
		} getOrElse {
			logger.warn("No such assessment group found: " + template.toText)
		}
	}
	
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
				if (existing needsUpdatingFrom assignment) {
					existing.copyFrom(assignment)
					session.update(existing)
				}
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
	
	def getSubmissionByUniId(assignment:Assignment, uniId:String) = {
		session.newCriteria[Submission]
				.add(Restrictions.eq("assignment", assignment))
				.add(Restrictions.eq("universityId", uniId))
				.uniqueResult
	}
	
	def getSubmission(id:String) = getById[Submission](id)
	
	def delete(submission:Submission) = {
		submission.assignment.submissions.remove(submission)
		session.delete(submission)
	}
	
	/**
	 * Deletes the OriginalityReport attached to this Submission if one
	 * exists. It flushes the session straight away because otherwise deletes
	 * don't always happen until after some insert operation that assumes
	 * we've deleted it.
	 */
	def deleteOriginalityReport(submission:Submission) {
		if (submission.originalityReport != null) {
			val report = submission.originalityReport
			submission.originalityReport = null
			session.delete(report)
			session.flush()
		}
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
			criteria(assignment.academicYear, upstream.moduleCode, upstream.assessmentGroup, assignment.occurrence)
					.uniqueResult
		}
	}
	
	def getAssessmentGroup(template:UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = find(template)
	
	def getUpstreamAssignment(id:String) = getById[UpstreamAssignment](id) 
	
    def getUpstreamAssignments(module:Module): Seq[UpstreamAssignment] = {
		session.newCriteria[UpstreamAssignment]
		        .add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
		        .addOrder(Order.asc("sequence"))
		        .list filter isInteresting
	}
	
	private def isInteresting(assignment:UpstreamAssignment) = {
		!(assignment.name contains "NOT IN USE")
	}

    def getAssessmentGroups(upstreamAssignment:UpstreamAssignment, academicYear:AcademicYear): Seq[UpstreamAssessmentGroup] = {
    	session.newCriteria[UpstreamAssessmentGroup]
                .add(Restrictions.eq("academicYear", academicYear))
                .add(Restrictions.eq("moduleCode", upstreamAssignment.moduleCode ))
                .add(Restrictions.eq("assessmentGroup", upstreamAssignment.assessmentGroup ))
                .list
    }
	
	private def criteria(academicYear:AcademicYear, moduleCode:String, assessmentGroup:String, occurrence:String) = 
		session.newCriteria[UpstreamAssessmentGroup]
				.add(Restrictions.eq("academicYear", academicYear))
				.add(Restrictions.eq("moduleCode", moduleCode ))
				.add(Restrictions.eq("assessmentGroup", assessmentGroup ))
				.add(Restrictions.eq("occurrence", occurrence ))
				
	private def debugReplace(template:UpstreamAssessmentGroup, universityIds:Seq[String]) {
		logger.debug("Setting %d members in group %s" format (universityIds.size, template.toText))
	}
}
