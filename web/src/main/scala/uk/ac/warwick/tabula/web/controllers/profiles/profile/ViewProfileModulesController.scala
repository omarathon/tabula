package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.StudentAssessmentProfileCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileModulesController extends AbstractViewProfileController {

  @RequestMapping(Array("/{member}/modules"))
  def viewByMemberMapping(
    @PathVariable member: Member,
    @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
  ): Mav = {
    mandatory(member) match {
      case student: StudentMember if student.mostSignificantCourseDetails.isDefined =>
        viewByCourse(student.mostSignificantCourseDetails.get, activeAcademicYear)
      case student: StudentMember if student.freshOrStaleStudentCourseDetails.nonEmpty =>
        viewByCourse(student.freshOrStaleStudentCourseDetails.lastOption.get, activeAcademicYear)
      case _ =>
        Redirect(Routes.Profile.identity(member))
    }
  }

  @RequestMapping(Array("/course/{studentCourseDetails}/{academicYear}/modules"))
  def viewByCourseMapping(
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    viewByCourse(studentCourseDetails, Some(mandatory(academicYear)))
  }

  private def viewByCourse(
    studentCourseDetails: StudentCourseDetails,
    activeAcademicYear: Option[AcademicYear]
  ): Mav = {
    val scyd = scydToSelect(studentCourseDetails, activeAcademicYear)
    val thisAcademicYear = scyd.get.academicYear
    val command = restricted(StudentAssessmentProfileCommand(mandatory(studentCourseDetails), thisAcademicYear))
    val studentBreakdown = command.map(_.apply())
    Mav("profiles/profile/modules_student",
      "hasPermission" -> command.nonEmpty,
      "command" -> command,
      "yearMark" -> studentBreakdown.map(_.yearMark),
      "weightedMeanYearMark" -> studentBreakdown.map(_.weightedMeanYearMark),
      "yearWeighting" -> studentBreakdown.flatMap(_.yearWeighting),
      "yearAbroad" -> scyd.exists(_.yearAbroad),
      "moduleRegistrationsAndComponents" -> studentBreakdown.map(_.modules).getOrElse(Seq.empty),
      "progressionDecisions" -> studentBreakdown.map(_.progressionDecisions.reverse).getOrElse(Seq.empty), // Most recent first
      "isSelf" -> (user.universityId.maybeText.getOrElse("") == studentCourseDetails.student.universityId),
      "member" -> studentCourseDetails.student,
      "hideYearWeightings" -> (scyd.exists(_.yearOfStudy == 1) && thisAcademicYear.startYear == 2019),
    ).crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.ModulesIdentifier): _*)
      .secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.modules(scyd)): _*)
  }

}
