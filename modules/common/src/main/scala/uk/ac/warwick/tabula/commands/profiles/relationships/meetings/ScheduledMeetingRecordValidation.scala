package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.helpers.StringUtils._

trait ScheduledMeetingRecordValidation {

	def sharedValidation(errors: Errors, title: String, meetingDateStr: String, meetingTimeStr: String, meetingEndTimeStr: String) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.hasText && title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")
		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		var meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " "+ meetingTimeStr)
		if(meetingDate.compareTo(DateTimePickerFormatter.parseDateTime(meetingDateStr + " "+ meetingEndTimeStr)) > -1){
			errors.rejectValue("meetingTimeStr", "meetingRecord.date.endbeforestart")
		}
		meetingDate match {
			case date:DateTime =>
				if (meetingDate.isBefore(DateTime.now.toDateTime))
					errors.rejectValue("meetingDate", "meetingRecord.date.past")
				else if (meetingDate.isAfter(DateTime.now.plusYears(MeetingRecord.MeetingTooOldThresholdYears).toDateTime))
					errors.rejectValue("meetingDate", "meetingRecord.date.futuristic")

			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}
}
