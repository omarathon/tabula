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
import uk.ac.warwick.tabula.data.model.{MeetingRecord, Member, StudentMember}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.Command
import scala.Some

class ViewProfileCommand(user: CurrentUser, profile: StudentMember) extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with Logging {

	if (user.isStudent && user.universityId != profile.universityId) {
		logger.info("Denying access for user " + user + " to view profile " + profile)
		throw new PermissionDeniedException(user, Permissions.Profiles.Read.Core, profile)
	}
}

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {

	var userLookup = Wire[UserLookupService]
	var smallGroupService = Wire[SmallGroupService]
	var memberNoteService = Wire[MemberNoteService]

	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	def getViewMeetingRecordCommand(member: Member, relationshipType: StudentRelationshipType): Option[Command[Seq[MeetingRecord]]] = {
		member.mostSignificantCourseDetails match {
			case Some(scd) => restricted(ViewMeetingRecordCommand(scd, user, relationshipType))
			case _ => {
				logger.warn("Member " + member.universityId + " has no most significant course details")
				None
			}
		}
	}

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping
	def viewProfile(
			@PathVariable("member") member: Member,
			@ModelAttribute("viewProfileCommand") profileCmd: Appliable[StudentMember],
			@RequestParam(value="meeting", required=false) openMeetingId: String,
			@RequestParam(defaultValue="", required=false) agentId: String) = {
		val profiledStudentMember = profileCmd.apply
		val isSelf = (profiledStudentMember.universityId == user.universityId)

		// Get all the enabled relationship types for a department
		val allRelationshipTypes =
			Option(member.homeDepartment)
				.map { _.displayedStudentRelationshipTypes }
				.getOrElse { relationshipService.allStudentRelationshipTypes }

		val relationshipMeetings =
			allRelationshipTypes.flatMap { relationshipType =>
				getViewMeetingRecordCommand(member, relationshipType).map { cmd =>
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
			if (securityService.can(user, Permissions.MemberNotes.Update, member)) memberNoteService.list(member)
			else if (securityService.can(user, Permissions.MemberNotes.Read, member)) memberNoteService.listNonDeleted(member)
			else null

		Mav("profile/view",
			"profile" -> profiledStudentMember,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"meetingsById" -> relationshipMeetings.map { case (relType, meetings) => (relType.id, meetings) },
			"openMeeting" -> openMeeting,
			"numSmallGroups" -> numSmallGroups,
			"memberNotes" -> memberNotes,
			"agent" -> agent)
		.crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}
