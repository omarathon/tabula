package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.helpers.StringUtils._

trait ScheduledMeetingRecordValidation {

	def sharedValidation(errors: Errors, title: String, meetingDate: DateTime) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.hasText && title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")
		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		meetingDate match {
			case date:DateTime => {
				if (meetingDate.isBefore(DateTime.now.toDateTime)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.past")
				} else if (meetingDate.isAfter(DateTime.now.plusYears(MeetingRecord.MeetingTooOldThresholdYears).toDateTime)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.futuristic")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}
}
