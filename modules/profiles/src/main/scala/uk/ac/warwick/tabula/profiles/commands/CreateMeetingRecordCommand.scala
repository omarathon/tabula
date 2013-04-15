package uk.ac.warwick.tabula.profiles.commands
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command,Description,SelfValidating}
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model.{Member,StudentRelationship,MeetingRecord}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.MeetingFormat

class CreateMeetingRecordCommand(val creator: Member, val relationship: StudentRelationship) extends Command[MeetingRecord] with SelfValidating with FormattedHtml {

	var title: String = _
	var description: String = _
	var meetingDate: LocalDate = DateTime.now.toLocalDate
	var format: MeetingFormat = _

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember.getOrElse(null))

	var dao = Wire.auto[MeetingRecordDao]

	def applyInternal() = {
		var meeting = new MeetingRecord(creator, relationship)
		meeting.title = title
		meeting.description = formattedHtml(description)
		meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay().withHourOfDay(12) // arbitrarily record as noon
		meeting.format = format
		dao.saveOrUpdate(meeting)
		meeting
	}

	def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		meetingDate match {
			case date:LocalDate => {
				if (meetingDate.isAfter(DateTime.now.toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.future")
				} else if (meetingDate.isBefore(DateTime.now.minusYears(5).toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.prehistoric")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}

	def describe(d: Description): Unit = {  }

}