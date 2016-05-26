package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.SearchProfilesCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav

abstract class OldViewProfileController extends ProfilesController {

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
			relationshipService.listAllStudentRelationshipTypesWithStudentMember(profiledStudentMember)
				.map(_.agentRole).distinct.toList

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
			if (securityService.can(user, Permissions.MemberNotes.Delete, profiledStudentMember)) memberNoteService.listNotes(profiledStudentMember)
			else if (securityService.can(user, Permissions.MemberNotes.Read, profiledStudentMember)) memberNoteService.listNonDeletedNotes(profiledStudentMember)
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
