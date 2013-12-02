package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{ MeetingRecord, Member, StudentMember }
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.{ Appliable, ViewViewableCommand }
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.Command
import scala.Some
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.web.Mav

class ViewProfileCommand(user: CurrentUser, profile: StudentMember) extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with Logging {

	if (user.isStudent && user.universityId != profile.universityId) {
		logger.info("Denying access for user " + user + " to view profile " + profile)
		throw new PermissionDeniedException(user, Permissions.Profiles.Read.Core, profile)
	}
}

@Controller
abstract class ViewProfileController extends ProfilesController {

	var userLookup = Wire[UserLookupService]
	var smallGroupService = Wire[SmallGroupService]
	var memberNoteService = Wire[MemberNoteService]
	var monitoringPointMeetingRelationshipTermService = Wire[MonitoringPointMeetingRelationshipTermService]

	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	def getViewMeetingRecordCommand(studentCourseDetails: Option[StudentCourseDetails], relationshipType: StudentRelationshipType): Option[Command[Seq[MeetingRecord]]] = {
		studentCourseDetails match {
			case Some(scd: StudentCourseDetails) => restricted(ViewMeetingRecordCommand(scd, optionalCurrentMember, relationshipType))
			case None => {
				logger.warn("No studentCourseDetails found")
				None
			}
		}
	}

	def viewProfileForCourse(
		studentCourseDetails: Option[StudentCourseDetails],
		openMeetingId: String,
		agentId: String,
		profiledStudentMember: uk.ac.warwick.tabula.data.model.StudentMember): uk.ac.warwick.tabula.web.Mav = {
		val isSelf = (profiledStudentMember.universityId == user.universityId)

		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		// Get meetings for all relationship types (not just the enabled ones for that dept)
		// because we show a relationship on the profile page if there is one
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
			if (securityService.can(user, Permissions.Profiles.Read.SmallGroups, profiledStudentMember))
				smallGroupService.findSmallGroupsByStudent(profiledStudentMember.asSsoUser).size
			else 0

		//Get all membernotes for student
		val memberNotes =
			if (securityService.can(user, Permissions.MemberNotes.Update, profiledStudentMember)) memberNoteService.list(profiledStudentMember)
			else if (securityService.can(user, Permissions.MemberNotes.Read, profiledStudentMember)) memberNoteService.listNonDeleted(profiledStudentMember)
			else null

		Mav("profile/view",
			"profile" -> profiledStudentMember,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"meetingsById" -> relationshipMeetings.map { case (relType, meetings) => (relType.id, meetings) },
			"meetingApprovalWillCreateCheckpoint" -> meetings.map(m => m.id -> monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(m)).toMap,
			"openMeeting" -> openMeeting,
			"numSmallGroups" -> numSmallGroups,
			"memberNotes" -> memberNotes,
			"agent" -> agent,
			"allRelationshipTypes" -> allRelationshipTypes,
			"studentCourseDetails" -> studentCourseDetails)
			.crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}
