package uk.ac.warwick.tabula.commands.attendance.manage.old

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.mutable
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Department, MeetingFormat, StudentRelationshipType}
import uk.ac.warwick.tabula.JavaImports._

trait MonitoringPointValidation {

	def validateName(errors: Errors, name: String, bindPoint: String) {
		if (!name.hasText) {
			errors.rejectValue(bindPoint, "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue(bindPoint, "monitoringPoint.name.toolong")
		}
	}

	def validateWeek(errors: Errors, week: Int, bindPoint: String) {
		week match {
			case y if y < 1  => errors.rejectValue(bindPoint, "monitoringPoint.week.min")
			case y if y > 52 => errors.rejectValue(bindPoint, "monitoringPoint.week.max")
			case _ =>
		}
	}

	def validateWeeks(errors: Errors, validFromWeek: Int, requiredFromWeek: Int, bindPoint: String) {
		if (validFromWeek > requiredFromWeek) {
			errors.rejectValue(bindPoint, "monitoringPoint.weeks")
		}
	}

	def validateTypeMeeting(errors: Errors,
		meetingRelationships: mutable.Set[StudentRelationshipType], meetingRelationshipsBindPoint: String,
		meetingFormats: mutable.Set[MeetingFormat], meetingFormatsBindPoint: String,
		meetingQuantity: Int, meetingQuantityBindPoint: String,
		dept: Department
	) {

		if (meetingRelationships.size == 0) {
			errors.rejectValue(meetingRelationshipsBindPoint, "monitoringPoint.meetingType.meetingRelationships.empty")
		} else {
			val invalidRelationships = meetingRelationships.filter(r => !dept.displayedStudentRelationshipTypes.contains(r))
			if (invalidRelationships.size > 0)
				errors.rejectValue(meetingRelationshipsBindPoint, "monitoringPoint.meetingType.meetingRelationships.invalid", invalidRelationships.mkString(", "))
		}

		if (meetingFormats.size == 0) {
			errors.rejectValue(meetingFormatsBindPoint, "monitoringPoint.meetingType.meetingFormats.empty")
		}

		if (meetingQuantity < 1) {
			errors.rejectValue(meetingQuantityBindPoint, "monitoringPoint.pointType.quantity")
		}
	}

	def validateTypeSmallGroup(errors: Errors,
		smallGroupEventModules: JSet[Module], smallGroupEventModulesBindPoint: String,
		isAnySmallGroupEventModules: Boolean,
		smallGroupEventQuantity: JInteger, smallGroupEventQuantityBindPoint: String,
		dept: Department
	) {

		if (smallGroupEventQuantity < 1) {
			errors.rejectValue(smallGroupEventQuantityBindPoint, "monitoringPoint.pointType.quantity")
		}

		if (!isAnySmallGroupEventModules && (smallGroupEventModules == null || smallGroupEventModules.size == 0)) {
			errors.rejectValue(smallGroupEventModulesBindPoint, "monitoringPoint.smallGroupType.smallGroupModules.empty")
		}

	}

	def validateTypeAssignmentSubmission(errors: Errors,
		isSpecificAssignments: Boolean,
		assignmentSubmissionQuantity: JInteger, assignmentSubmissionQuantityBindPoint: String,
		assignmentSubmissionModules: JSet[Module], assignmentSubmissionModulesBindPoint: String,
		assignmentSubmissionAssignments: JSet[Assignment], assignmentSubmissionAssignmentsBindPoint: String,
		dept: Department
	) {

		if (isSpecificAssignments) {
			if (assignmentSubmissionAssignments == null || assignmentSubmissionAssignments.isEmpty) {
				errors.rejectValue(assignmentSubmissionAssignmentsBindPoint, "monitoringPoint.assingmentSubmissionType.assignmentSubmissionAssignments.empty")
			}
		} else {
			if (assignmentSubmissionQuantity < 1) {
				errors.rejectValue(assignmentSubmissionQuantityBindPoint, "monitoringPoint.pointType.quantity")
			}

			if (assignmentSubmissionModules == null || assignmentSubmissionModules.isEmpty) {
				errors.rejectValue(assignmentSubmissionModulesBindPoint, "monitoringPoint.assingmentSubmissionType.assignmentSubmissionModules.empty")
			}
		}
	}
}
