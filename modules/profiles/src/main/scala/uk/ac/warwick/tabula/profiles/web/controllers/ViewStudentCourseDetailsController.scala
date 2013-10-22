package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.services.MemberNoteService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.MeetingRecord

class ViewStudentCourseDetailsCommand(user: CurrentUser, studentCourseDetails: StudentCourseDetails) extends ViewViewableCommand(Permissions.Profiles.Read.Core, studentCourseDetails) with Logging {

	if (user.isStudent && user.universityId != studentCourseDetails.student.universityId) {
		logger.info("Denying access for user " + user + " to view course details " + studentCourseDetails.scjCode)
		throw new PermissionDeniedException(user, Permissions.Profiles.Read.Core, studentCourseDetails)
	}
}

@Controller
@RequestMapping(Array("/view/course/{scjCode}"))
class ViewStudentCourseDetailsController extends ProfilesController {

	var userLookup = Wire[UserLookupService]
	var smallGroupService = Wire[SmallGroupService]
	var memberNoteService = Wire[MemberNoteService]

	def getViewMeetingRecordCommand(studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType): Option[Command[Seq[MeetingRecord]]] = {
		restricted(ViewMeetingRecordCommand(studentCourseDetails, optionalCurrentMember, relationshipType))
	}

	@RequestMapping
	def viewCourseDetails(
			@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
			@RequestParam(value="meeting", required=false) openMeetingId: String,
			@RequestParam(defaultValue="", required=false) agentId: String) = {
		val isSelf = (studentCourseDetails.student.universityId == user.universityId)

		// Get all the enabled relationship types for a department
		val allRelationshipTypes =
			Option(studentCourseDetails.department)
				.map { _.displayedStudentRelationshipTypes }
				.getOrElse { relationshipService.allStudentRelationshipTypes }

		val relationshipMeetings =
			allRelationshipTypes.flatMap { relationshipType =>
				getViewMeetingRecordCommand(studentCourseDetails, relationshipType).map { cmd =>
					(relationshipType, cmd.apply())
				}
			}.toMap

		val meetings = relationshipMeetings.values.flatten
		val openMeeting = meetings.find(m => m.id == openMeetingId).getOrElse(null)

		val agent = userLookup.getUserByWarwickUniId(agentId)

		// the number of small groups that the student is a member of
		val numSmallGroups =
			if (securityService.can(user, Permissions.Profiles.Read.SmallGroups, studentCourseDetails.student))
				smallGroupService.findSmallGroupsByStudent(studentCourseDetails.student.asSsoUser).size
			else 0

		Mav("profile/course",
			"studentCourseDetails" -> studentCourseDetails,
			"profile" -> studentCourseDetails.student,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"meetingsById" -> relationshipMeetings.map { case (relType, meetings) => (relType.id, meetings) },
			"openMeeting" -> openMeeting,
			"numSmallGroups" -> numSmallGroups,
			"agent" -> agent)
		.crumbs(Breadcrumbs.Profile(studentCourseDetails.student, isSelf))
	}
}
