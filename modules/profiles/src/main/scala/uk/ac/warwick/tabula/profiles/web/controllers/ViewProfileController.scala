package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.web.Mav


class ViewProfileCommand(user: CurrentUser, profile: StudentMember)
	extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with Logging {

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

	def getViewMeetingRecordCommand(
		studentCourseDetails: Option[StudentCourseDetails],
		relationshipType: StudentRelationshipType
	): Option[Command[Seq[AbstractMeetingRecord]]] = {
		studentCourseDetails match {
			case Some(scd: StudentCourseDetails) => restricted(ViewMeetingRecordCommand(scd, optionalCurrentMember, relationshipType))
			case None => None
		}
	}

	def viewProfileForCourse(
		studentCourseDetails: Option[StudentCourseDetails],
		studentCourseYearDetails: Option[StudentCourseYearDetails],
		openMeetingId: String,
		agentId: String,
		profiledStudentMember: StudentMember): Mav = {
		val isSelf = profiledStudentMember.universityId == user.universityId

		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		// Get meetings for all relationship types (not just the enabled ones for that dept)
		// because we show a relationship on the profile page if there is one
		val relationshipMeetings =
			allRelationshipTypes.flatMap { relationshipType =>
				getViewMeetingRecordCommand(studentCourseDetails, relationshipType).map { cmd =>
					(relationshipType, cmd.apply())
				}
			}.toMap

		val relationshipTypes: List[String] =
			if (currentMember.isStudent)
				relationshipService.listAllStudentRelationshipTypesWithStudentMember(currentMember.asInstanceOf[StudentMember])
					.map(_.agentRole).distinct.toList
			else relationshipService.listAllStudentRelationshipTypesWithMember(currentMember).map (_.studentRole + "s").distinct.toList

		def relationshipTypesFormatted = relationshipTypes match {
			case Nil => ""
			case singleFormat :: Nil => singleFormat
			case _ => Seq(relationshipTypes.init.mkString(", "), relationshipTypes.last).mkString(" and ")
		}

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
			"viewerRelationshipTypes" -> relationshipTypesFormatted,
			"profile" -> profiledStudentMember,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"meetingsById" -> relationshipMeetings.map { case (relType, m) => (relType.id, m) },
			"meetingApprovalWillCreateCheckpoint" -> meetings.map {
				case (meeting: MeetingRecord) => meeting.id -> monitoringPointMeetingRelationshipTermService.willCheckpointBeCreated(meeting)
				case (meeting: ScheduledMeetingRecord) => meeting.id -> false
			}.toMap,
			"openMeeting" -> openMeeting,
			"numSmallGroups" -> numSmallGroups,
			"memberNotes" -> memberNotes,
			"agent" -> agent,
			"allRelationshipTypes" -> allRelationshipTypes,
			"studentCourseDetails" -> studentCourseDetails,
			"studentCourseYearDetails" -> studentCourseYearDetails)
			.crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}

	def studentCourseYearFromYear(studentCourseDetails: StudentCourseDetails, year: AcademicYear) =
		studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == year).seq.headOption

}
