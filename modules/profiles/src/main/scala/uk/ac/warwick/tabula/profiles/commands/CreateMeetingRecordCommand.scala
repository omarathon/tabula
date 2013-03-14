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

	@BeanProperty val meeting = new MeetingRecord(creator, relationship)

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember.getOrElse(null))
	
	var dao = Wire.auto[MeetingRecordDao]

	def applyInternal() = {
		dao.saveOrUpdate(meeting)
		meeting
	}

	def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "meeting.title", "NotEmpty")
		
		meeting.meetingDate match {
			case date:DateTime => {
				if (meeting.meetingDate.isAfter(DateTime.now)) {
					errors.rejectValue("meeting.meetingDate", "meetingRecord.date.future")
				} else if (meeting.meetingDate.isBefore(DateTime.now.minusYears(5))) {
					errors.rejectValue("meeting.meetingDate", "meetingRecord.date.prehistoric")
				}
			}
			case null => errors.rejectValue("meeting.meetingDate", "meetingRecord.date.missing")
			case _ => errors.rejectValue("meeting.meetingDate", "typeMismatch.org.joda.time.DateTime")
		}
	}

	def describe(d: Description): Unit = {  }

}