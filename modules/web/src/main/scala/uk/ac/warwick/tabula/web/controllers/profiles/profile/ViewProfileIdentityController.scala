package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringMemberNoteServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileIdentityController extends AbstractViewProfileController
	with AutowiringMemberNoteServiceComponent {

	@RequestMapping(Array("/{member}"))
	def viewByMemberMapping(
		@PathVariable member: Member,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		mandatory(member) match {
			case student: StudentMember if student.mostSignificantCourseDetails.isDefined =>
				viewByCourse(student.mostSignificantCourseDetails.get, activeAcademicYear)
			case _ =>
				Mav("profiles/profile/identity_staff").crumbs(breadcrumbs(member, ProfileBreadcrumbs.Profile.IdentityIdentifier): _*)
		}
	}

	@RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}"))
	def viewByCourseMapping(
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val activeAcademicYear: Option[AcademicYear] = Some(mandatory(academicYear))
		viewByCourse(studentCourseDetails, activeAcademicYear)
	}

	private def viewByCourse(
		studentCourseDetails: StudentCourseDetails,
		activeAcademicYear: Option[AcademicYear]
	): Mav = {
		val (memberNotes: Seq[MemberNote], extenuatingCircumstances: Seq[ExtenuatingCircumstances]) = {
			if (securityService.can(user, Permissions.MemberNotes.Delete, studentCourseDetails.student)) {
				(memberNoteService.listNotes(studentCourseDetails.student), memberNoteService.listExtenuatingCircumstances(studentCourseDetails.student))
			}	else if (securityService.can(user, Permissions.MemberNotes.Read, studentCourseDetails.student)) {
				(memberNoteService.listNonDeletedNotes(studentCourseDetails.student), memberNoteService.listNonDeletedExtenuatingCircumstances(studentCourseDetails.student))
			} else {
				(Seq(), Seq())
			}
		}

		Mav("profiles/profile/identity_student",
			"member" -> studentCourseDetails.student,
			"courseDetails" -> (Seq(studentCourseDetails) ++ studentCourseDetails.student.freshStudentCourseDetails.filterNot(_ == studentCourseDetails)),
			"memberNotes" -> memberNotes,
			"extenuatingCircumstances" -> extenuatingCircumstances
		).crumbs(breadcrumbs(studentCourseDetails.student, ProfileBreadcrumbs.Profile.IdentityIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.identity(scyd)): _*)
	}

}
