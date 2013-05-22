package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.Command
import data.model._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.{ UniversityId, AcademicYear, DateFormats }
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.commands.SelfValidating


case class UpstreamGroupOption(
	assignmentId: String,
	name: String,
	cats: Option[String], // TODO joke about cats and string
	sequence: String,
	occurrence: String,
	memberCount: Int,
	isLinked: Boolean)

/**
 * Common behaviour
 */
abstract class ModifyAssignmentCommand(val module: Module) extends Command[Assignment] with SharedAssignmentProperties with SelfValidating {

	var service = Wire.auto[AssignmentService]
	var membershipService = Wire.auto[AssignmentMembershipService]
	var userLookup = Wire.auto[UserLookupService]

	def assignment: Assignment

	@Length(max = 200)
	@NotEmpty(message = "{NotEmpty.assignmentName}")
	var name: String = _

	var openDate: DateTime = DateTime.now.withTime(0, 0, 0, 0)

	var closeDate: DateTime = openDate.plusWeeks(2).withTime(12, 0, 0, 0)

	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime)

	def getAcademicYearString =
		if (academicYear != null)
			academicYear.toString()
		else
			""

	// start complicated membership stuff

	/** linked SITS assignments. Optional. */
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()
	var assessmentGroupItems: JList[AssessmentGroupItem] = JArrayList()

	/**
	 * If copying from existing Assignment, this must be a DEEP COPY
	 * with changes copied back to the original UserGroup, don't pass
	 * the same UserGroup around because it'll just cause Hibernate
	 * problems. This copy should be transient.
	 *
	 * Changes to members are done via includeUsers and excludeUsers, since
	 * it is difficult to bind additions and removals directly to a collection
	 * with Spring binding.
	 */
	var members: UserGroup = new UserGroup

	// items added here are added to members.includeUsers.
	var includeUsers: JList[String] = JArrayList()

	// items added here are either removed from members.includeUsers or added to members.excludeUsers.
	var excludeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	var massAddUsers: String = _

	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("\\s+") map (_.trim) filterNot (_.isEmpty)

	///// end of complicated membership stuff

	// can be set to false if that's not what you want.
	var prefillFromRecent = true

	var prefillAssignment: Assignment = _

	private var _prefilled: Boolean = _
	def prefilled = _prefilled

	// can be overridden in concrete implementations to provide additional validation
	def contextSpecificValidation(errors: Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)

		// TAB-255 Guard to avoid SQL error - if it's null or gigantic it will fail validation in other ways.
		if (name != null && name.length < 3000) {
			val duplicates = service.getAssignmentByNameYearModule(name, academicYear, module).filterNot { _ eq assignment }
			for (duplicate <- duplicates.headOption) {
				errors.rejectValue("name", "name.duplicate.assignment", Array(name), "")
			}
		}

		if (!openEnded && openDate.isAfter(closeDate)) {
			errors.reject("closeDate.early")
		}

		validateShared(errors)
	}

	// Called by controller after Spring has done its basic binding, to do more complex
	// stuff like resolving users into groups and putting them in the members UserGroup.
	def afterBind() {

		def addUserId(item: String) {
			val user = userLookup.getUserByUserId(item)
			if (user.isFoundUser && null != user.getWarwickId) {
				includeUsers.add(user.getUserId)
			}
		}

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

		val membersUserIds = Option(assignment).map(membershipService.determineMembershipUsers(_).map(_.getUserId)).getOrElse(List())

		// add includeUsers to members.includeUsers
		((includeUsers map { _.trim } filterNot { _.isEmpty }).distinct) foreach { userId =>
				if (members.excludeUsers contains userId) {
					members.unexcludeUser(userId)
				} else if (!(membersUserIds contains userId)) {
					// TAB-399 need to check if the user is already in the group before adding
						members.addUser(userId)
				}
		}

		// for excludeUsers, either remove from previously-added users or add to excluded users.
		((excludeUsers map { _.trim } filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.includeUsers contains userId) members.removeUser(userId)
			else members.excludeUser(userId)
		}


		// empty these out to make it clear that we've "moved" the data into members
		massAddUsers = ""
		// TAB-399
		//includeUsers.clear()
		// HFC-327
		//excludeUsers.clear()
	}

	def copyTo(assignment: Assignment) {
		assignment.name = name
		assignment.openDate = openDate
		assignment.closeDate = closeDate
		assignment.academicYear = academicYear
		assignment.feedbackTemplate = feedbackTemplate

		if (this.assignment != null) {
			persistAssessmentGroupChanges()
		}

		copySharedTo(assignment: Assignment)

		if (assignment.members == null) assignment.members = new UserGroup
		assignment.members copyFrom members
	}



	def persistAssessmentGroupChanges() {
			val removedGroups = assignment.assessmentGroups.filterNot(assessmentGroups.contains(_))
			removedGroups.foreach(membershipService.delete(_))

			val newGroups = assessmentGroupItems.map{item =>
				val assessmentGroup = new AssessmentGroup
				assessmentGroup.occurrence =  item.occurrence
				assessmentGroup.upstreamAssignment =  item.upstreamAssignment
				assessmentGroup.assignment = assignment
				membershipService.save(assessmentGroup)
				assessmentGroup
			}

			assignment.assessmentGroups = assessmentGroups ++ newGroups
	}

	def prefillFromRecentAssignment() {
		if (prefillAssignment != null) {
			copyNonspecificFrom(prefillAssignment)
		} else {
			if (prefillFromRecent) {
				for (a <- service.recentAssignment(module.department)) {
					copyNonspecificFrom(a)
					_prefilled = true
				}
			}
		}
	}

	/**
	 * Copy just the fields that it might be useful to
	 * prefill. The assignment passed in might typically be
	 * another recently created assignment, that may have good
	 * initial values for submission options.
	 */
	def copyNonspecificFrom(assignment: Assignment) {
		openDate = assignment.openDate
		closeDate = assignment.closeDate
		copySharedFrom(assignment)
	}

	def copyFrom(assignment: Assignment) {
		name = assignment.name
		academicYear = assignment.academicYear
		feedbackTemplate = assignment.feedbackTemplate
		assessmentGroups = assignment.assessmentGroups
		if (assignment.members != null) {
			members copyFrom assignment.members
		}
		copyNonspecificFrom(assignment)
	}


	/**
	 * Retrieve a list of possible upstream assignments and occurrences
	 * to link to SITS data. Includes the upstream assignment and the
	 * occurrence ID, plus some info like the number of members there.
	 */
	def upstreamGroupOptions: Seq[UpstreamGroupOption] = {
		val assignments = membershipService.getUpstreamAssignments(module)
		assignments flatMap { assignment =>
			val groups = membershipService.getAssessmentGroups(assignment, academicYear)
			groups map { group =>
				UpstreamGroupOption(
					assignmentId = assignment.id,
					name = assignment.name,
					cats = assignment.cats,
					sequence = assignment.sequence,
					occurrence = group.occurrence,
					memberCount = group.members.members.size,
					isLinked = assessmentGroups.exists(g =>
						g.upstreamAssignment.id == assignment.id && g.occurrence == group.occurrence))
			}
		}
	}

	/**
	 * Returns a sequence of MembershipItems
	 */
	def membershipDetails =
		membershipService.determineMembership(upstreamAssessmentGroups, Option(members))

	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = {
		if(academicYear == null || assessmentGroups == null){
			Seq()
		}
		else {
			val validAssignments = assessmentGroups.filterNot(group => group.upstreamAssignment == null || group.occurrence == null)
			val groups = validAssignments.flatMap{group =>
				val template = new UpstreamAssessmentGroup
				template.academicYear = academicYear
				template.assessmentGroup = group.upstreamAssignment.assessmentGroup
				template.moduleCode = group.upstreamAssignment.moduleCode
				template.occurrence = group.occurrence
				membershipService.getAssessmentGroup(template)
			}
			groups
		}
	}
}

class AssessmentGroupItem(){
	var occurrence: String = _
	var upstreamAssignment: UpstreamAssignment =_
}