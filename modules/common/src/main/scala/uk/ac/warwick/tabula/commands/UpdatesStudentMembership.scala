package uk.ac.warwick.tabula.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{MembershipInfo, UserLookupService, AssignmentMembershipService}
import scala.Some
import scala.Some

trait UpdatesStudentMembership extends CurrentAcademicYear {

	var userLookup = Wire.auto[UserLookupService]
	var membershipService = Wire.auto[AssignmentMembershipService]

	// needs a module to determine the possible options from SITS
	val module : Module
	val exisitingGroups: Option[Seq[UpstreamAssessmentGroup]]
	val existingMembers: Option[UserGroup]

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups()

	/** linked assessment groups, which are to be persisted with the assignment.
		*  They can be used to lookup SITS UpstreamAssessmentGroups on demand,
		*  when required to lookup users.
		*/
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	// bind property for items to be added to members.includeUsers
	@transient var includeUsers: JList[String] = JArrayList()

	// bind property for items to either be removed from members.includeUsers or added to members.excludeUsers
	@transient var excludeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	@transient var massAddUsers: String = _

	/** bind property for changing assessment groups */
	@transient var upstreamGroups: JList[UpstreamGroup] = JArrayList()

	/**
	 * If copying from existing object, this must be a DEEP COPY
	 * with changes copied back to the original UserGroup, don't pass
	 * the same UserGroup around because it'll just cause Hibernate
	 * problems. This copy should be transient.
	 *
	 * Changes to members are done via includeUsers and excludeUsers, since
	 * it is difficult to bind additions and removals directly to a collection
	 * with Spring binding.
	 */
	var members: UserGroup = new UserGroup


	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("(\\s|[^A-Za-z\\d\\-_\\.])+") map (_.trim) filterNot (_.isEmpty)

	def afterBind() {
		updateMembership()
		updateAssessmentGroups()
	}

	private def addUserId(item: String) {
		val user = userLookup.getUserByUserId(item)
		if (user.isFoundUser && null != user.getWarwickId) {
			includeUsers.add(user.getUserId)
		}
	}

	/**
	 * Convert Spring-bound user lists into an explicit UserGroup
	 */
	private def updateMembership() {
		// parse items from textarea into includeUsers collection
		for (item <- massAddUsersEntries) {
			if (UniversityId.isValid(item)) {
				val user = userLookup.getUserByWarwickUniId(item)
				if (user.isFoundUser) {
					includeUsers.add(user.getUserId)
				} else {
					addUserId(item)
				}
			} else {
				addUserId(item)
			}
		}

		// get implicit membership list from upstream
		val memberUsers = exisitingGroups.map(membershipService.determineMembershipUsers(_, existingMembers)).getOrElse(Seq())
		val membersUserIds = memberUsers.map(_.getUserId)


		// unexclude from previously excluded users, or explicitly include
		((includeUsers.asScala map { _.trim } filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.excludeUsers contains userId) {
				members.unexcludeUser(userId)
			} else if (!(membersUserIds contains userId)) {
				// TAB-399 only add if not already a member of a linked UpstreamGroup
				members.addUser(userId)
			}
		}

		// uninclude from previously-added users, or explicitly exclude
		((excludeUsers.asScala map { _.trim } filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.includeUsers contains userId) members.removeUser(userId)
			else members.excludeUser(userId)
		}

		// empty these out to make it clear that we've "moved" the data into members
		massAddUsers = ""
		includeUsers = JArrayList()
		excludeUsers = JArrayList()
	}

	/**
	 * Build a seq of available upstream groups for upstream assignments on this module
	 */
	lazy val availableUpstreamGroups: Seq[UpstreamGroup] = {
		val upstreamAssignments = membershipService.getUpstreamAssignments(module)

		for {
			ua <- membershipService.getUpstreamAssignments(module)
			uag <- membershipService.getUpstreamAssessmentGroups(ua, academicYear)
		} yield new UpstreamGroup(ua, uag)
	}


	/** get UAGs, populated with membership, from the currently stored assessmentGroups */
	def linkedUpstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = {
		if(academicYear == null || assessmentGroups == null){
			Seq()
		}
		else {
			val validGroups = assessmentGroups.asScala.filterNot(group => group.upstreamAssignment == null || group.occurrence == null).toList

			validGroups.flatMap{group =>
				val template = new UpstreamAssessmentGroup
				template.academicYear = academicYear
				template.assessmentGroup = group.upstreamAssignment.assessmentGroup
				template.moduleCode = group.upstreamAssignment.moduleCode
				template.occurrence = group.occurrence
				membershipService.getUpstreamAssessmentGroup(template)
			}
		}
	}

	/**
	 * Returns a sequence of MembershipItems
	 */
	def membershipInfo : MembershipInfo = membershipService.determineMembership(linkedUpstreamAssessmentGroups, Option(members))

}

/**
 * convenience classes
 */
class UpstreamGroup(val upstreamAssignment: UpstreamAssignment, val group: UpstreamAssessmentGroup) {
	val id = upstreamAssignment.id + ";" + group.id

	val name = upstreamAssignment.name
	val memberCount = group.memberCount
	val cats = upstreamAssignment.cats
	val occurrence = group.occurrence
	val sequence = upstreamAssignment.sequence

	def isLinked(assessmentGroups: JList[AssessmentGroup]) = assessmentGroups.asScala.exists(ag =>
		ag.upstreamAssignment.id == upstreamAssignment.id && ag.occurrence == group.occurrence)

	override def toString = "upstreamAssignment: " + upstreamAssignment.id + ", occurrence: " + group.occurrence
}


class UpstreamGroupPropertyEditor extends AbstractPropertyEditor[UpstreamGroup] {
	var membershipService = Wire.auto[AssignmentMembershipService]

	override def fromString(id: String) = {
		def explode = throw new IllegalArgumentException("No unique upstream group which matches id " + id + " is available")

		id.split(";") match {
			case Array(uaId: String, groupId: String) =>
				val ua = membershipService.getUpstreamAssignment(uaId).getOrElse(explode)
				val uag = membershipService.getUpstreamAssessmentGroup(groupId).getOrElse(explode)
				new UpstreamGroup(ua, uag)
			case _ => explode
		}
	}

	override def toString(ug: UpstreamGroup) = ug.id
}
