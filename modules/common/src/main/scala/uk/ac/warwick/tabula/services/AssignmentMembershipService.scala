package uk.ac.warwick.tabula.services

import collection.JavaConverters._

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.{Order, Restrictions}


trait AssignmentMembershipService {
	def save(group: AssessmentGroup): Unit
	def delete(group: AssessmentGroup): Unit
	def getAssessmentGroup(id: String): Option[AssessmentGroup]
	def getAssessmentGroup(template: AssessmentGroup): Option[AssessmentGroup]
	def getUpstreamAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]
	def getUpstreamAssessmentGroup(id:String): Option[UpstreamAssessmentGroup]
	def getUpstreamAssignment(id: String): Option[UpstreamAssignment]
	def getUpstreamAssignment(group: UpstreamAssessmentGroup): Option[UpstreamAssignment]

	/**
	 * Get all UpstreamAssignments that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getUpstreamAssessmentGroups.
	 */
	def getUpstreamAssignments(module: Module): Seq[UpstreamAssignment]
	def getUpstreamAssignments(department: Department): Seq[UpstreamAssignment]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getUpstreamAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

	def save(assignment: UpstreamAssignment): UpstreamAssignment
	def save(group: UpstreamAssessmentGroup)
	def replaceMembers(group: UpstreamAssessmentGroup, universityIds: Seq[String])

	def getEnrolledAssignments(user: User): Seq[Assignment]

	def countMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Int
	def countMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Int
	def countMembershipUsers(assignment: Assignment): Int

	def determineMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): AssignmentMembershipInfo
	def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[User]
	def determineMembershipUsers(assignment: Assignment): Seq[User]

	def isStudentMember(user: User, upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Boolean
}

@Service(value = "assignmentMembershipService")
class AssignmentMembershipServiceImpl
	extends AssignmentMembershipService
	with AssignmentMembershipMethods
	with Daoisms
	with Logging {

	@Autowired var userLookup: UserLookupService = _

	def getEnrolledAssignments(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a
				from Assignment a
				left join fetch a.assessmentGroups ag
				where
					(1 = (
						select 1 from uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup uag
						where uag.moduleCode = ag.upstreamAssignment.moduleCode
							and uag.assessmentGroup = ag.upstreamAssignment.assessmentGroup
							and uag.academicYear = a.academicYear
							and uag.occurrence = ag.occurrence
							and :universityId in elements(uag.members.staticIncludeUsers)
					) or :userId in elements(a.members.includeUsers))
					and :userId not in elements(a.members.excludeUsers)
					and a.deleted = false and a.archived = false
																 """).setString("universityId", user.getWarwickId()).setString("userId", user.getUserId()).seq

	def replaceMembers(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
		if (debugEnabled) debugReplace(template, universityIds)
		getUpstreamAssessmentGroup(template).map { group =>
			val collection = group.members.staticIncludeUsers
			collection.clear()
			collection.addAll(universityIds.asJava)
		} getOrElse {
			logger.warn("No such assessment group found: " + template.toString)
		}
	}

	private def debugReplace(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
		logger.debug("Setting %d members in group %s" format (universityIds.size, template.toString))
	}

	/**
	 * Tries to find an identical UpstreamAssignment in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment: UpstreamAssignment): Option[UpstreamAssignment] = session.newCriteria[UpstreamAssignment]
		.add(Restrictions.eq("moduleCode", assignment.moduleCode))
		.add(Restrictions.eq("sequence", assignment.sequence))
		.uniqueResult

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
		.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
		.add(Restrictions.eq("academicYear", group.academicYear))
		.add(Restrictions.eq("moduleCode", group.moduleCode))
		.add(Restrictions.eq("occurrence", group.occurrence))
		.uniqueResult

	def find(group: AssessmentGroup): Option[AssessmentGroup] = session.newCriteria[AssessmentGroup]
		.add(Restrictions.eq("assignment", group.assignment))
		.add(Restrictions.eq("upstreamAssignment", group.upstreamAssignment))
		.add(Restrictions.eq("occurrence", group.occurrence))
		.uniqueResult

	def save(group:AssessmentGroup) = session.saveOrUpdate(group)

	def save(assignment: UpstreamAssignment): UpstreamAssignment =
		find(assignment)
			.map { existing =>
			if (existing needsUpdatingFrom assignment) {
				existing.copyFrom(assignment)
				session.update(existing)
			}

			existing
		}
			.getOrElse { session.save(assignment); assignment }

	def save(group: UpstreamAssessmentGroup) =
		find(group)
			.map { existing =>
			// do nothing. nothing else to update except members
			//session.update(existing.id, group)
		}
			.getOrElse { session.save(group) }

	def getAssessmentGroup(id:String) = getById[AssessmentGroup](id)
	def getAssessmentGroup(template: AssessmentGroup): Option[AssessmentGroup] = find(template)
	def getUpstreamAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = find(template)
	def getUpstreamAssessmentGroup(id:String) = getById[UpstreamAssessmentGroup](id)

	def delete(group: AssessmentGroup) {
		group.assignment.assessmentGroups.remove(group)
		session.delete(group)
		session.flush()
	}

	def getUpstreamAssignment(id: String) = getById[UpstreamAssignment](id)

	def getUpstreamAssignment(group: UpstreamAssessmentGroup) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.eq("moduleCode", group.moduleCode))
			.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
			.uniqueResult
	}

	def getUpstreamAssignments(module: Module) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.addOrder(Order.asc("sequence"))
			.seq filter isInteresting
	}

	def getUpstreamAssignments(department: Department) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.eq("departmentCode", department.code.toUpperCase))
			.addOrder(Order.asc("moduleCode"))
			.addOrder(Order.asc("sequence"))
			.seq filter isInteresting
	}

	def getStudentFeedback(assignment: Assignment, uniId: String) = {
		assignment.findFullFeedback(uniId)
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def countFullFeedback(assignment: Assignment): Int = {  //join f.attachments a
		session.createQuery("""select count(*) from Feedback f
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
			.setEntity("assignment", assignment)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	private def isInteresting(assignment: UpstreamAssignment) = {
		!(assignment.name contains "NOT IN USE")
	}

	def getUpstreamAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("academicYear", academicYear))
			.add(Restrictions.eq("moduleCode", upstreamAssignment.moduleCode))
			.add(Restrictions.eq("assessmentGroup", upstreamAssignment.assessmentGroup))
			.seq
	}
}



class AssignmentMembershipInfo(val items: Seq[MembershipItem]) {

	def	sitsCount = items.filter(_.itemType == SitsType).size
	def	totalCount = items.filterNot(_.itemType == ExcludeType).size
	def includeCount = items.filter(_.itemType == IncludeType).size
	def excludeCount = items.filter(_.itemType == ExcludeType).size
	def usedIncludeCount = items.filter(i => i.itemType == IncludeType && !i.extraneous).size
	def usedExcludeCount = items.filter(i => i.itemType == ExcludeType && !i.extraneous).size

}

trait AssignmentMembershipMethods { self: AssignmentMembershipServiceImpl =>

	def determineMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): AssignmentMembershipInfo = {
		val sitsUsers =
			upstream.flatMap { _.members.members }
				.distinct
				.par.map { id =>
				id -> userLookup.getUserByWarwickUniId(id)
			}.seq

		def toUserMap(userIds: JList[String]) =  userIds.asScala map { id => id -> userLookup.getUserByUserId(id) }
		def toUsers(group: UserGroup) = (toUserMap(group.includeUsers), toUserMap(group.excludeUsers))
		val (includes, excludes) = others map toUsers getOrElse (Nil, Nil)

		// convert lists of Users to lists of MembershipItems that we can render neatly together.

		val includeItems = makeIncludeItems(includes, sitsUsers)
		val excludeItems = makeExcludeItems(excludes, sitsUsers)
		val sitsItems = makeSitsItems(includes, excludes, sitsUsers)

		new AssignmentMembershipInfo(includeItems ++ excludeItems ++ sitsItems)
	}

	/**
	 * Returns just a list of User objects who are on this assessment group.
	 */
	def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[User] = {
		determineMembership(upstream, others).items filter notExclude map toUser filter notNull
	}

	/**
	 * Returns a simple list of User objects for students who are enrolled on this assignment. May be empty.
	 */
	def determineMembershipUsers(assignment: Assignment): Seq[User] = {
		determineMembershipUsers(assignment.upstreamAssessmentGroups, Option(assignment.members))
	}

	/**
	 * May overestimate
	 */
	def countMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]) = {
		val sitsUsers = upstream.flatMap { _.members.members }.distinct

		val includes = others map { _.includeUsers.asScala } getOrElse Nil
		val excludes = others map { _.excludeUsers.asScala } getOrElse Nil

		((sitsUsers ++ includes) diff excludes).size
	}

	/**
	 * Returns just a list of User objects who are on this assessment group.
	 */
	def countMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]) =
		countMembership(upstream, others)

	/**
	 * Returns a simple count of students who are enrolled on this assignment
	 */
	def countMembershipUsers(assignment: Assignment): Int =
		countMembershipUsers(assignment.upstreamAssessmentGroups, Option(assignment.members))

	def isStudentMember(user: User, upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Boolean = {
		if (others map {_.excludeUsers contains user.getUserId } getOrElse false) false
		else if (others map { _.includeUsers contains user.getUserId } getOrElse false) true
		else upstream exists {
			_.members.staticIncludeUsers contains user.getWarwickId //Yes, definitely Uni ID when checking SITS group
		}
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
					itemType = IncludeType,
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
					itemType = ExcludeType,
					extraneous = extraneous)
		}

	private def makeSitsItems(includes: Seq[Pair[String, User]], excludes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		sitsUsers filterNot in(includes) filterNot in(excludes) map {
			case (id, user) =>
				MembershipItem(
					user = user,
					universityId = universityId(user, Some(id)),
					userId = userId(user, None),
					itemType = SitsType,
					extraneous = false)
		}

	private def universityId(user: User, fallback: Option[String]) = option(user) map { _.getWarwickId } orElse fallback
	private def userId(user: User, fallback: Option[String]) = option(user) map { _.getUserId } orElse fallback

	private def option(user: User): Option[User] = user match {
		case FoundUser(u) => Some(user)
		case _ => None
	}

	private def toUser(item: MembershipItem) = item.user
	private def notExclude(item: MembershipItem) = item.itemType != ExcludeType
	private def notNull[A](any: A) = { any != null }
}

abstract class MembershipItemType(val value: String)
case object SitsType extends MembershipItemType("sits")
case object IncludeType extends MembershipItemType("include")
case object ExcludeType extends MembershipItemType("exclude")

/** Item in list of members for displaying in view.
	*/
case class MembershipItem(
	 user: User,
	 universityId: Option[String],
	 userId: Option[String],
	 itemType: MembershipItemType, // sits, include or exclude
	 /**
		* If include type, this item adds a user who's already in SITS.
		* If exclude type, this item excludes a user who isn't in the list anyway.
		*/
	 extraneous: Boolean) {
	def itemTypeString = itemType.value
}

