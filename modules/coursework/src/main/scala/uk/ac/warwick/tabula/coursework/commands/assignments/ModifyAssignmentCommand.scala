package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList}
import scala.collection.JavaConverters._

import org.hibernate.validator.constraints.{Length, NotEmpty}
import org.joda.time.DateTime
import org.springframework.validation.Errors

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssignmentService
import scala.Some


/**
 * Common behaviour
 */
 abstract class ModifyAssignmentCommand(val module: Module,val updateStudentMembershipGroupIsUniversityIds:Boolean=false) extends Command[Assignment]
	with SharedAssignmentProperties with SelfValidating with UpdatesStudentMembership with SpecifiesGroupType with CurrentAcademicYear {

	var service = Wire.auto[AssignmentService]

	def assignment: Assignment

	@Length(max = 200)
	@NotEmpty(message = "{NotEmpty.assignmentName}")
	var name: String = _

	var openDate: DateTime = DateTime.now.withTime(0, 0, 0, 0)

	var closeDate: DateTime = openDate.plusWeeks(2).withTime(12, 0, 0, 0)

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


	private def addUserId(item: String) {
		val user = userLookup.getUserByUserId(item)
		if (user.isFoundUser && null != user.getWarwickId) {
			includeUsers.add(user.getUserId)
		}
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
			group.assignment = assignment
		}

		copySharedTo(assignment: Assignment)

		if (assignment.members == null) assignment.members = UserGroup.ofUsercodes
		assignment.members.copyFrom(members)
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
			assessmentGroups.exists( ag => ug.upstreamAssignment == ag.assessmentComponent && ag.occurrence == ug.occurrence )
		})
	}

	def copyFrom(assignment: Assignment) {
		name = assignment.name
		academicYear = assignment.academicYear
		feedbackTemplate = assignment.feedbackTemplate
		if (assignment.members != null) {
			members = assignment.members.duplicate
		}
		copyNonspecificFrom(assignment)
	}

	val existingGroups = Option(assignment).map(_.upstreamAssessmentGroups)
	val existingMembers = Option(assignment).map(_.members)

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups(){
		assessmentGroups = upstreamGroups.asScala.flatMap ( ug => {
			val template = new AssessmentGroup
			template.assessmentComponent = ug.upstreamAssignment
			template.occurrence = ug.occurrence
			template.assignment = assignment
			membershipService.getAssessmentGroup(template) orElse Some(template)
		}).distinct.asJava
	}

}