package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.profiles.{CreateScheduledMeetingRecordCommand, ViewMeetingRecordCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.{MeetingRecordAcademicYearFiltering, ProfilesController}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/schedule/create"))
class CreateScheduledMeetingRecordController extends ProfilesController
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with MeetingRecordAcademicYearFiltering
	with AutowiringTermServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("allRelationships")
	def allRelationships(@PathVariable studentCourseDetails: StudentCourseDetails,
											 @PathVariable relationshipType: StudentRelationshipType) = {

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
	}

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@ModelAttribute("command")
	def getCommand(@PathVariable relationshipType: StudentRelationshipType,
				   @PathVariable studentCourseDetails: StudentCourseDetails,
				   @RequestParam(value="relationship", required = false) relationship: StudentRelationship) =  {
		relationshipService.findCurrentRelationships(mandatory(relationshipType), mandatory(studentCourseDetails)) match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val chosenRelationship = relationship match {
					case r: StudentRelationship => r
					case _ => relationships.find(rel => rel.agentMember.map(_.universityId) == Some(user.universityId))
										.getOrElse(relationships.head)
				}

				CreateScheduledMeetingRecordCommand(currentMember, chosenRelationship, relationships.size > 1)
		}
	}

	@RequestMapping(method=Array(GET, HEAD), params=Array("iframe"))
	def getIframe(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		form(cmd, studentCourseDetails, iframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
	 @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
	 @PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		form(cmd, studentCourseDetails)
	}

	private def form(
		cmd: Appliable[ScheduledMeetingRecord],
		studentCourseDetails: StudentCourseDetails,
		iframe: Boolean = false
	) = {
		val mav = Mav("profiles/related_students/meeting/schedule",
			"returnTo" -> getReturnTo(Routes.profile.view(studentCourseDetails.student)),
			"isModal" -> ajax,
			"iframe" -> iframe,
			"formats" -> MeetingFormat.members
		)
		if(ajax)
			mav.noLayout()
		else if (iframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("iframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable relationshipType: StudentRelationshipType,
		@ModelAttribute("viewMeetingRecordCommand") viewCommand: Option[Appliable[Seq[AbstractMeetingRecord]]]
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails, iframe = true)
		} else {
			val modifiedMeeting = cmd.apply()
			val meetingList = viewCommand match {
				case None => Seq()
				case Some(command) => command.apply().filterNot(meetingNotInAcademicYear(AcademicYear.guessSITSAcademicYearByDate(modifiedMeeting.meetingDate)))
			}
			Mav("profiles/related_students/meeting/list",
				"studentCourseDetails" -> studentCourseDetails,
				"role" -> relationshipType,
				"meetings" -> meetingList,
				"meetingApprovalWillCreateCheckpoint" -> meetingList.map {
					case (meeting: MeetingRecord) => meeting.id -> attendanceMonitoringMeetingRecordService.getCheckpoints(meeting).nonEmpty
					case (meeting: ScheduledMeetingRecord) => meeting.id -> false
				}.toMap,
				"viewerUser" -> user,
				"viewerMember" -> currentMember,
				"openMeeting" -> modifiedMeeting).noLayout()
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable studentCourseDetails: StudentCourseDetails
	) = {
		if (errors.hasErrors) {
			form(cmd, studentCourseDetails)
		} else {
			cmd.apply()
			Redirect(Routes.profile.view(studentCourseDetails.student))
		}
	}

}
