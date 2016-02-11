package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.profiles.SearchProfilesCommand
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, PermissionDeniedException}


class ViewProfileCommand(user: CurrentUser, profile: Member)
	extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with Logging {

	private val viewingOwnProfile = user.apparentUser.getWarwickId == profile.universityId
	private val viewerInSameDepartment = Option(user.apparentUser.getDepartmentCode)
		.map(_.toLowerCase)
		.exists(deptCode => profile.touchedDepartments.map(_.code).contains(deptCode))

	if (!user.god && !viewingOwnProfile && (user.isStudent || profile.isStaff && !viewerInSameDepartment)) {
		logger.info("Denying access for user " + user + " to view profile " + profile)
		throw new PermissionDeniedException(user, Permissions.Profiles.Read.Core, profile)
	}
}

@Controller
abstract class ViewProfileController extends ProfilesController {

	var userLookup = Wire[UserLookupService]
	var smallGroupService = Wire[SmallGroupService]
	var memberNoteService = Wire[MemberNoteService]
	var assignmentService = Wire[AssessmentService]
	implicit var termService = Wire[TermService]

	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	def viewProfileForCourse(
		studentCourseDetails: Option[StudentCourseDetails],
		studentCourseYearDetails: Option[StudentCourseYearDetails],
		openMeetingId: String,
		agentId: String,
		profiledStudentMember: StudentMember
		): Mav = {
		val isSelf = profiledStudentMember.universityId == user.universityId

		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes

		val relationshipTypes: List[String] =
			if (currentMember.isStudent)
				relationshipService.listAllStudentRelationshipTypesWithStudentMember(currentMember.asInstanceOf[StudentMember])
					.map(_.agentRole).distinct.toList
			else relationshipService.listAllStudentRelationshipTypesWithMember(currentMember).map(_.studentRole + "s").distinct.toList

		def relationshipTypesFormatted = relationshipTypes match {
			case Nil => ""
			case singleFormat :: Nil => singleFormat
			case _ => Seq(relationshipTypes.init.mkString(", "), relationshipTypes.last).mkString(" and ")
		}

		val agent = userLookup.getUserByWarwickUniId(agentId)

		// the number of small groups that the student is a member of
		val numSmallGroups =
			if (securityService.can(user, Permissions.Profiles.Read.SmallGroups, profiledStudentMember))
				smallGroupService.findSmallGroupsByStudent(profiledStudentMember.asSsoUser).size
			else 0

		// Get all membernotes for student
		val memberNotes =
			if (securityService.can(user, Permissions.MemberNotes.Delete, profiledStudentMember)) memberNoteService.list(profiledStudentMember)
			else if (securityService.can(user, Permissions.MemberNotes.Read, profiledStudentMember)) memberNoteService.listNonDeleted(profiledStudentMember)
			else null

		Mav("profiles/profile/view",
			"viewerRelationshipTypes" -> relationshipTypesFormatted,
			"profile" -> profiledStudentMember,
			"viewerUser" -> user,
			"viewerMember" -> currentMember,
			"isSelf" -> isSelf,
			"openMeetingId" -> openMeetingId,
			"numSmallGroups" -> numSmallGroups,
			"memberNotes" -> memberNotes,
			"agent" -> agent,
			"allRelationshipTypes" -> allRelationshipTypes,
			"studentCourseDetails" -> studentCourseDetails,
			"studentCourseYearDetails" -> studentCourseYearDetails
		).crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}

	def viewProfileForStaff(profiledMember: StaffMember): Mav = {

		val isSelf = profiledMember.universityId == user.universityId

		val relationshipTypes: Seq[StudentRelationshipType] =
			relationshipService.listAllStudentRelationshipTypesWithMember(currentMember)

		val smallGroups = smallGroupService.findReleasedSmallGroupsByTutor(user)


		val marking = assignmentService.getAssignmentWhereMarker(user.apparentUser)

		Mav("profiles/profile/view",
			"viewerRelationshipTypes" -> relationshipTypes,
			"profile" -> profiledMember,
			"viewerUser" -> user,
			"viewerMember" -> currentMember,
			"isSelf" -> isSelf,
			"isStaff" -> profiledMember.isStaff,
			"marker" -> profiledMember.asSsoUser,
			"smallGroups" -> smallGroups,
			"marking" -> marking
		).crumbs(Breadcrumbs.Profile(profiledMember, isSelf))
	}

	def studentCourseYearFromYear(studentCourseDetails: StudentCourseDetails, year: AcademicYear) =
		studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == year).seq.headOption

}
