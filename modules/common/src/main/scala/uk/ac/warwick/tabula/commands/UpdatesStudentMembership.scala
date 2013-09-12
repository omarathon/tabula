package uk.ac.warwick.tabula.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AssignmentMembershipInfo, UserLookupService, AssignmentMembershipService}
import scala.collection.mutable.ListBuffer
import uk.ac.warwick.userlookup.User

trait UpdatesStudentMembership {

	this : CurrentAcademicYear with SpecifiesGroupType =>

	var userLookup = Wire.auto[UserLookupService]
	var membershipService = Wire.auto[AssignmentMembershipService]

	// needs a module to determine the possible options from SITS
	val module : Module
	val existingGroups: Option[Seq[UpstreamAssessmentGroup]]
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
	var members: UnspecifiedTypeUserGroup = {
		val ug = if (updateStudentMembershipGroupIsUniversityIds){
			UserGroup.ofUniversityIds

		}else{
			UserGroup.ofUsercodes
		}
		ug.userLookup = userLookup
		ug
	}


	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("(\\s|[^A-Za-z\\d\\-_\\.])+") map (_.trim) filterNot (_.isEmpty)

	def afterBind() {
		updateMembership()
		updateAssessmentGroups()
	}

	private def addUserFromUserId(userId: String, addTo: ListBuffer[User]) {
		val user = userLookup.getUserByUserId(userId)
		if (user.isFoundUser && null != user.getWarwickId) {
			addTo += user
		}
	}

	private def addIfValidUser(userString: String, addTo: ListBuffer[User]) {
		if (UniversityId.isValid(userString)) {
			val user = userLookup.getUserByWarwickUniId(userString)
			if (user.isFoundUser) {
				addTo += user
			} else {
				addUserFromUserId(userString, addTo)
			}
		} else {
			addUserFromUserId(userString, addTo)
		}
	}

	/**
	 * Convert Spring-bound user lists into an explicit UserGroup
	 */
	private def updateMembership() {

		// a list to hold the users we're adding
		val usersToAdd = ListBuffer[User]()

		// parse items from textarea into usersToAdd list
		for (item <- massAddUsersEntries) {
			addIfValidUser(item, usersToAdd)
		}

		// now add the users from includeUsers
		for (includedUser <- includeUsers.asScala){
			addIfValidUser(includedUser, usersToAdd)
		}

		// now get implicit membership list from upstream
		val upstreamMembers = existingGroups.map(membershipService.determineMembershipUsers(_, existingMembers)).getOrElse(Seq())

		for (user<-usersToAdd.distinct){
			if (members.excludes.contains(user)){
				members.unexclude(user)
			}
			else if (!upstreamMembers.contains(user)){
				// TAB-399 only add if not already a member of a linked UpstreamGroup
				members.add(user)
			}
		}

		// uninclude from previously-added users, or explicitly exclude
		val usersToExclude = userLookup.getUsersByUserIds(JArrayList((excludeUsers.asScala map { _.trim } filterNot { _.isEmpty }).distinct)).asScala map(_._2)
		for (exclude<-usersToExclude){
			if (members.users contains exclude){
				members.remove(exclude)
			}else{
				members.exclude(exclude)
			}
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
	def membershipInfo : AssignmentMembershipInfo = membershipService.determineMembership(linkedUpstreamAssessmentGroups, Option(members))

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
trait SpecifiesGroupType{
	val updateStudentMembershipGroupIsUniversityIds:Boolean
}