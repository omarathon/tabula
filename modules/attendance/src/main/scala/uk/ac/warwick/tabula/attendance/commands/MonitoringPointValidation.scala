package uk.ac.warwick.tabula.attendance.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.mutable
import uk.ac.warwick.tabula.data.model.{Department, MeetingFormat, StudentRelationshipType}

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

		if (meetingQuantity == 0) {
			errors.rejectValue(meetingQuantityBindPoint, "monitoringPoint.meetingType.meetingQuantity")
		}
	}
}
