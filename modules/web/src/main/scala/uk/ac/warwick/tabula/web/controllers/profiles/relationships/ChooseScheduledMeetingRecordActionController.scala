package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.data.model.{ScheduledMeetingRecord, StudentCourseDetails, StudentRelationshipType}


@Controller
@RequestMapping(Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/schedule/{meetingRecord}/chooseaction"))
class ChooseScheduledMeetingRecordActionController extends ProfilesController {

	@RequestMapping(params = Array("action=confirm"))
	def confirm(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = {
		Redirect(Routes.scheduledMeeting.confirm(meetingRecord, studentCourseDetails, relationshipType))
	}

	@RequestMapping(params = Array("action=reschedule"))
	def reschedule(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = {
		Redirect(Routes.scheduledMeeting.reschedule(meetingRecord, studentCourseDetails, relationshipType))
	}

	@RequestMapping(params = Array("action=missed"))
	def missed(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = {
		Redirect(Routes.scheduledMeeting.missed(meetingRecord, studentCourseDetails, relationshipType))
	}

	@RequestMapping
	def none(
		@PathVariable meetingRecord: ScheduledMeetingRecord
	) = {
		Redirect(
			Routes.oldProfile.view(
				meetingRecord.relationship.studentMember.getOrElse(throw new IllegalArgumentException),
				meetingRecord
			)
		)
	}
}
