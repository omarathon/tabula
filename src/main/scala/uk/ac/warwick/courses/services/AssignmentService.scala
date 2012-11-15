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
import uk.ac.warwick.courses.helpers.{ FoundUser, Logging }

/**
 * Service providing access to Assignments and related objects.
 *
 * TODO this is getting a bit monstrous and all-encompassing.
 */
trait AssignmentService {
	def getAssignmentById(id: String): Option[Assignment]
	def save(assignment: Assignment)

	def saveSubmission(submission: Submission)
	def getSubmissionByUniId(assignment: Assignment, uniId: String): Option[Submission]
	def getSubmission(id: String): Option[Submission]

	def delete(submission: Submission): Unit
	
	def deleteOriginalityReport(attachment: FileAttachment): Unit
	def saveOriginalityReport(attachment: FileAttachment): Unit
	
	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]

	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]]

	def getEnrolledAssignments(user: User): Seq[Assignment]
	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def getAssessmentGroup(assignment: Assignment): Option[UpstreamAssessmentGroup]
	def getAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]

	def getUpstreamAssignment(id: String): Option[UpstreamAssignment]

	/**
	 * Get all UpstreamAssignments that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getAssessmentGroups.
	 */
	def getUpstreamAssignments(module: Module): Seq[UpstreamAssignment]
	def getUpstreamAssignments(department: Department): Seq[UpstreamAssignment]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

	def save(assignment: UpstreamAssignment)
	def save(group: UpstreamAssessmentGroup)
	def replaceMembers(group: UpstreamAssessmentGroup, universityIds: Seq[String])

	def determineMembership(upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Seq[MembershipItem]
	def determineMembershipUsers(upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Seq[User]
	def determineMembershipUsers(assignment: Assignment): Seq[User]

	def isStudentMember(user: User, upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Boolean

	def getStudentFeedback(assignment: Assignment, warwickId: String): Option[Feedback]
}

@Service(value = "assignmentService")
class AssignmentServiceImpl extends AssignmentService with AssignmentMembershipMethods with Daoisms with Logging {
	import Restrictions._

	@Autowired var userLookup: UserLookupService = _
	@Autowired var auditEventIndexService: AuditEventIndexService = _

	def getAssignmentById(id: String) = getById[Assignment](id)
	def save(assignment: Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission: Submission) = {
		session.saveOrUpdate(submission)
		session.flush()
	}

	def replaceMembers(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
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
	def find(assignment: UpstreamAssignment): Option[UpstreamAssignment] = session.newCriteria[UpstreamAssignment]
		.add(Restrictions.eq("moduleCode", assignment.moduleCode))
		.add(Restrictions.eq("sequence", assignment.sequence))
		.uniqueResult

	def save(assignment: UpstreamAssignment) =
		find(assignment)
			.map { existing =>
				if (existing needsUpdatingFrom assignment) {
					existing.copyFrom(assignment)
					session.update(existing)
				}
			}
			.getOrElse { session.save(assignment) }

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
		.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
		.add(Restrictions.eq("academicYear", group.academicYear))
		.add(Restrictions.eq("moduleCode", group.moduleCode))
		.add(Restrictions.eq("occurrence", group.occurrence))
		.uniqueResult

	def save(group: UpstreamAssessmentGroup) =
		find(group)
			.map { existing =>
				// do nothing. nothing else to update except members
				//session.update(existing.id, group)
			}
			.getOrElse { session.save(group) }

	def getSubmissionByUniId(assignment: Assignment, uniId: String) = {
		session.newCriteria[Submission]
			.add(Restrictions.eq("assignment", assignment))
			.add(Restrictions.eq("universityId", uniId))
			.uniqueResult
	}

	def getSubmission(id: String) = getById[Submission](id)

	def delete(submission: Submission) = {
		submission.assignment.submissions.remove(submission)
		session.delete(submission)
		// force delete now, just for the cases where we re-insert in the same session
		// (i.e. when a student is resubmitting work). [HFC-385#comments]
		session.flush()
	}

	/**
	 * Deletes the OriginalityReport attached to this Submission if one
	 * exists. It flushes the session straight away because otherwise deletes
	 * don't always happen until after some insert operation that assumes
	 * we've deleted it.
	 */
	def deleteOriginalityReport(attachment: FileAttachment) {
		if (attachment.originalityReport != null) {
			val report = attachment.originalityReport
			attachment.originalityReport = null
			session.delete(report)
			session.flush()
		}
	}
	
	def saveOriginalityReport(attachment: FileAttachment) {
		attachment.originalityReport.attachment = attachment
		session.save(attachment.originalityReport)
	}
	
	def getEnrolledAssignments(user: User): Seq[Assignment] = {
		session.createSQLQuery("""
              select * from 
              (
                (
                  -- manually included users
                  select distinct a.* from usergroupinclude ugi 
                  join assignment a on a.membersgroup_id = ugi.group_id
                  where ugi.usercode = :userId
                  and a.deleted = 0
                  and a.archived = 0
                )
                union
                (
                  -- sits assessment groups
                  select distinct a.* from assignment a
                  join upstreamassignment ua on a.upstream_id = ua.id
                  join upstreamassessmentgroup uag 
                    on uag.modulecode = ua.modulecode 
                    and uag.assessmentgroup = ua.assessmentgroup
                    and uag.academicyear = a.academicyear
                    and uag.occurrence = a.occurrence
                  join usergroupstatic ugs on uag.membersgroup_id = ugs.group_id
                  where ugs.usercode = :universityId
                  and a.deleted = 0
                  and a.archived = 0
                )
              )
              where id not in
              (
                -- manually excluded users
                select distinct a.id from usergroupexclude uge 
                join assignment a on a.membersgroup_id = uge.group_id
                where uge.usercode = :userId
              )
		""")
		    .addEntity(classOf[Assignment])
            .setString("universityId", user.getWarwickId())
            .setString("userId", user.getUserId())
            .list.asInstanceOf[JList[Assignment]]
	}

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment] =
		session.createQuery("""select distinct a from Assignment a
				join a.feedbacks as f
				where f.universityId = :universityId
				and f.released=true""")
			.setString("universityId", universityId)
			.list.asInstanceOf[JList[Assignment]]

	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment] =
		session.createQuery("""select distinct a from Assignment a
				join a.submissions as f
				where f.universityId = :universityId""")
			.setString("universityId", universityId)
			.list.asInstanceOf[JList[Assignment]]

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module) = {
		session.createQuery("from Assignment where name=:name and academicYear=:year and module=:module and deleted=0")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.list().asInstanceOf[JList[Assignment]]
	}

	/* get users whose feedback is not published and who have not submitted work suspected 
	 * of being plagiarised */
	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]] = {
		//val uniIds = assignment.unreleasedFeedback.map { _.universityId }
		//uniIds.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }

		val plagiarisedSubmissions = assignment.submissions.filter { submission => submission.suspectPlagiarised }
		val plagiarisedIds = plagiarisedSubmissions.map { _.universityId }
		val unreleasedIds = assignment.unreleasedFeedback.map { _.universityId }
		val unplagiarisedUnreleasedIds = unreleasedIds.filter { uniId => !plagiarisedIds.contains(uniId) }
		unplagiarisedUnreleasedIds.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }
	}

	def recentAssignment(department: Department) = {
		//auditEventIndexService.recentAssignment(department)
		session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(Restrictions.eq("m.department", department))
			.add(Restrictions.isNotNull("createdDate"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(1)
			.uniqueResult
	}

	def getAssignmentsByName(partialName: String, department: Department) = {
		session.newCriteria[Assignment]
			.createAlias("module", "mod")
			.add(Restrictions.eq("mod.department", department))
			.add(Restrictions.ilike("name", "%" + partialName + "%"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(15)
			.list
	}

	def getAssessmentGroup(assignment: Assignment): Option[UpstreamAssessmentGroup] = {
		val upstream = assignment.upstreamAssignment
		if (upstream == null) None
		else criteria(assignment.academicYear, upstream.moduleCode, upstream.assessmentGroup, assignment.occurrence).uniqueResult
	}

	def getAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = find(template)

	def getUpstreamAssignment(id: String) = getById[UpstreamAssignment](id)

	def getUpstreamAssignments(module: Module) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.addOrder(Order.asc("sequence"))
			.list filter isInteresting
	}

	def getUpstreamAssignments(department: Department) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.eq("departmentCode", department.code.toUpperCase))
			.addOrder(Order.asc("moduleCode"))
			.addOrder(Order.asc("sequence"))
			.list filter isInteresting
	}

	def getStudentFeedback(assignment: Assignment, uniId: String) = {
		assignment.findFeedback(uniId)
	}

	private def isInteresting(assignment: UpstreamAssignment) = {
		!(assignment.name contains "NOT IN USE")
	}

	def getAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("academicYear", academicYear))
			.add(Restrictions.eq("moduleCode", upstreamAssignment.moduleCode))
			.add(Restrictions.eq("assessmentGroup", upstreamAssignment.assessmentGroup))
			.list
	}

	private def criteria(academicYear: AcademicYear, moduleCode: String, assessmentGroup: String, occurrence: String) =
		session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("academicYear", academicYear))
			.add(Restrictions.eq("moduleCode", moduleCode))
			.add(Restrictions.eq("assessmentGroup", assessmentGroup))
			.add(Restrictions.eq("occurrence", occurrence))

	private def debugReplace(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
		logger.debug("Setting %d members in group %s" format (universityIds.size, template.toText))
	}
}

trait AssignmentMembershipMethods { self: AssignmentServiceImpl =>

	def determineMembership(upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Seq[MembershipItem] = {

		val sitsUsers = upstream map { upstream =>
			upstream.members.members map { id =>
				id -> userLookup.getUserByWarwickUniId(id)
			}
		} getOrElse Nil
		val includes = others.includeUsers map { id => id -> userLookup.getUserByUserId(id) }
		val excludes = others.excludeUsers map { id => id -> userLookup.getUserByUserId(id) }

		// convert lists of Users to lists of MembershipItems that we can render neatly together.

		val includeItems = makeIncludeItems(includes, sitsUsers)
		val excludeItems = makeExcludeItems(excludes, sitsUsers)
		val sitsItems = makeSitsItems(includes, excludes, sitsUsers)

		includeItems ++ excludeItems ++ sitsItems
	}

	/**
	 * Returns just a list of User objects who are on this assessment group.
	 */
	def determineMembershipUsers(upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Seq[User] = {
		determineMembership(upstream, others) filter notExclude map toUser filter notNull
	}

	/**
	 * Returns a simple list of User objects for students who are enrolled on this assignment. May be empty.
	 */
	def determineMembershipUsers(assignment: Assignment): Seq[User] = {
		determineMembershipUsers(assignment.assessmentGroup, assignment.members)
	}

	def isStudentMember(user: User, upstream: Option[UpstreamAssessmentGroup], others: UserGroup): Boolean = {
		if (others.excludeUsers contains user.getUserId) false
		else if (others.includeUsers contains user.getUserId) true
		else upstream map {
			_.members.staticIncludeUsers contains user.getWarwickId //Yes, definitely Uni ID when checking SITS group
		} getOrElse false // not in any group at all
	}

	private def sameUserIdAs(user: User) = (other: Pair[String, User]) => { user.getUserId == other._2.getUserId }
	private def in(seq: Seq[Pair[String, User]]) = (other: Pair[String, User]) => { seq exists sameUserIdAs(other._2) }

	private def makeIncludeItems(includes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		includes map {
			case (id, user) =>
				val extraneous = sitsUsers exists sameUserIdAs(user)
				MembershipItem(
					user = user,
					universityId = universityId(user, None),
					userId = userId(user, Some(id)),
					itemType = "include",
					extraneous = extraneous)
		}

	private def makeExcludeItems(excludes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		excludes map {
			case (id, user) =>
				val extraneous = !(sitsUsers exists sameUserIdAs(user))
				MembershipItem(
					user = user,
					universityId = universityId(user, None),
					userId = userId(user, Some(id)),
					itemType = "exclude",
					extraneous = extraneous)
		}

	private def makeSitsItems(includes: Seq[Pair[String, User]], excludes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		sitsUsers filterNot in(includes) filterNot in(excludes) map {
			case (id, user) =>
				MembershipItem(
					user = user,
					universityId = universityId(user, Some(id)),
					userId = userId(user, None),
					itemType = "sits",
					extraneous = false)
		}

	private def universityId(user: User, fallback: Option[String]) = option(user) map { _.getWarwickId } orElse fallback
	private def userId(user: User, fallback: Option[String]) = option(user) map { _.getUserId } orElse fallback

	private def option(user: User): Option[User] = user match {
		case FoundUser(u) => Some(user)
		case _ => None
	}

	private def toUser(item: MembershipItem) = item.user
	private def notExclude(item: MembershipItem) = item.itemType != "exclude"
	private def notNull[T](any: T) = { any != null }
}

/** Item in list of members for displaying in view. */
case class MembershipItem(
	user: User,
	universityId: Option[String],
	userId: Option[String],
	itemType: String, // sits, include or exclude
	/**
	 * If include type, this item adds a user who's already in SITS.
	 * If exclude type, this item excludes a user who isn't in the list anyway.
	 */
	extraneous: Boolean)
