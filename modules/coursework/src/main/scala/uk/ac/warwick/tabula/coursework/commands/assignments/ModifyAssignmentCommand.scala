package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.JavaConversions.{asScalaBuffer, bufferAsJavaList, seqAsJavaList}

import org.hibernate.validator.constraints.{Length, NotEmpty}
import org.joda.time.DateTime
import org.springframework.validation.Errors

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.{Command, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, UpstreamAssessmentGroup, UpstreamAssignment, UserGroup}
import uk.ac.warwick.tabula.data.model.forms.AssessmentGroup
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AssignmentService, UserLookupService}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor


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

	/** 1:1 linked SITS assignment and MAV_OCCURRENCE as per the value in SITS.
	 *  Optional, superceded by assessmentGroups
	 */
	@deprecated var upstreamAssignment: UpstreamAssignment = _
	@deprecated var occurrence: String = _

	/** linked assessment groups, which are to be persisted with the assignment.
	 *  They can be used to lookup SITS UpstreamAssessmentGroups on demand,
	 *  when required to lookup users.
	 */
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()
	
	/** bind property for changing assessment groups */
	@transient var upstreamGroups: JList[UpstreamGroup] = JArrayList()

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

	// bind property for items to be added to members.includeUsers
	@transient var includeUsers: JList[String] = JArrayList()
	
	// bind property for items to either be removed from members.includeUsers or added to members.excludeUsers
	@transient var excludeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	@transient var massAddUsers: String = _

	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split ("[\\s\\W]+") map (_.trim) filterNot (_.isEmpty)

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

	/**
	 *  
	 * Called by controller after Spring has done its basic binding, to do more complex
	 * stuff like resolving users into groups and putting them in the members UserGroup.
	 */
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

		// for excludeUsers, either remove from previously-added users or add to excluded users
		((excludeUsers map { _.trim } filterNot { _.isEmpty }).distinct) foreach { userId =>
			if (members.includeUsers contains userId) members.removeUser(userId)
			else members.excludeUser(userId)
		}

		// empty these out to make it clear that we've "moved" the data into members
		massAddUsers = ""
		
		// now deal with groups
		updateAssessmentGroups()
	}

	def copyTo(assignment: Assignment) {
		assignment.name = name
		assignment.openDate = openDate
		assignment.closeDate = closeDate
		assignment.academicYear = academicYear
		assignment.feedbackTemplate = feedbackTemplate
		
		assignment.assessmentGroups.clear
		assignment.assessmentGroups.addAll(assessmentGroups)
		for (group <- assignment.assessmentGroups if group.assignment == null) {
			group.assignment = assignment // only required for a new assignment
		}
		
		assignment.upstreamAssignment = upstreamAssignment
		assignment.occurrence = occurrence

		copySharedTo(assignment: Assignment)

		if (assignment.members == null) assignment.members = new UserGroup
		assignment.members copyFrom members
	}

	/**
	 *  Convert Spring-bound upstream group references to an #AssessmentGroup buffer,
	 *  and persist if required.
	 */
	def updateAssessmentGroups(persist: Boolean = false) {
		assessmentGroups = upstreamGroups.flatMap ( ug => {
			val template = new AssessmentGroup
			template.upstreamAssignment = ug.upstreamAssignment
			template.occurrence = ug.occurrence
			template.assignment = assignment
			membershipService.getAssessmentGroup(template) orElse Some(template)
		})
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
	
	def copyGroupsFrom(assignment: Assignment) {
		assessmentGroups = assignment.assessmentGroups
		upstreamGroups.addAll(availableUpstreamGroups filter { ug => 
			assessmentGroups.exists( ag => ug.upstreamAssignment == ag.upstreamAssignment && ag.occurrence == ug.occurrence )
		})
	}

	def copyFrom(assignment: Assignment) {
		name = assignment.name
		academicYear = assignment.academicYear
		feedbackTemplate = assignment.feedbackTemplate
		upstreamAssignment = assignment.upstreamAssignment
		occurrence = assignment.occurrence
		if (assignment.members != null) {
			members copyFrom assignment.members
		}
		copyNonspecificFrom(assignment)
	}

	/**
	 * Build a seq of available upstream groups for upstream assignments on this module
	 */
	lazy val availableUpstreamGroups: Seq[UpstreamGroup] = {
		val upstreamAssignments = membershipService.getUpstreamAssignments(module)
		
		upstreamAssignments.flatMap { ua =>
			val uags = membershipService.getUpstreamAssessmentGroups(ua, academicYear)
			uags map { uag =>
				new UpstreamGroup(ua, uag)
			}
		}
	}

	
	/** get UAGs, populated with membership, from the currently stored assessmentGroups */
	def linkedUpstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] = {
		if(academicYear == null || assessmentGroups == null){
			Seq()
		}
		else {
			val validGroups = assessmentGroups.filterNot(group => group.upstreamAssignment == null || group.occurrence == null).toList
			
			validGroups.map{group =>
				val template = new UpstreamAssessmentGroup
				template.academicYear = academicYear
				template.assessmentGroup = group.upstreamAssignment.assessmentGroup
				template.moduleCode = group.upstreamAssignment.moduleCode
				template.occurrence = group.occurrence
				membershipService.getUpstreamAssessmentGroup(template)
			}.flatten
		}
	}
	
	/**
	 * Returns a sequence of MembershipItems
	 */
	def assignmentMembership = membershipService.determineMembership(linkedUpstreamAssessmentGroups, Option(members))
}


/**
 * convenience class
 */
class UpstreamGroup(val upstreamAssignment: UpstreamAssignment, val group: UpstreamAssessmentGroup) {
	val id = upstreamAssignment.id + ";" + group.id
	
	val name = upstreamAssignment.name
	val memberCount = group.memberCount
	val cats = upstreamAssignment.cats
	val occurrence = group.occurrence
	val sequence = upstreamAssignment.sequence
	
	def isLinked(assessmentGroups: JList[AssessmentGroup]) = assessmentGroups.exists(ag =>
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
