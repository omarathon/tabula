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

class CreateMeetingRecordCommand(val creator: Member, val relationship: StudentRelationship) extends Command[MeetingRecord] with SelfValidating {

	@BeanProperty var title: String = _
	@BeanProperty var description: String = _
	@BeanProperty var meetingDate: DateTime = _

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember.getOrElse(null))
	
	var dao = Wire.auto[MeetingRecordDao]

	def applyInternal() = {
		var meeting = new MeetingRecord(creator, relationship)
		meeting.title = title
		meeting.description = description
		meeting.meetingDate = meetingDate
		dao.saveOrUpdate(meeting)
		meeting
	}

	def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		
		meetingDate match {
			case date:DateTime => {
				if (meetingDate.isAfter(DateTime.now)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.future")
				} else if (meetingDate.isBefore(DateTime.now.minusYears(5))) {
					errors.rejectValue("meetingDate", "meetingRecord.date.prehistoric")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}

	def describe(d: Description): Unit = {  }

}