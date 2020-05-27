package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.StudentAssessmentCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, NormalLoadLookup}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.{ExamsController, StudentCourseYearDetailsBreadcrumbs}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/{studentCourseDetails}/assessmentdetails"))
class StudentAssessmentBreakdownController extends ExamsController
  with StudentCourseYearDetailsBreadcrumbs with DepartmentScopedController with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
  with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent with AutowiringNormalCATSLoadServiceComponent
  with TaskBenchmarking {

  override val departmentPermission: Permission = Permissions.Department.ExamGrids

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))


  @ModelAttribute("command")
  def command(@PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear): StudentAssessmentCommand.Command = {
    StudentAssessmentCommand(studentCourseDetails, mandatory(academicYear))
  }

  @ModelAttribute("weightings")
  def weightings(@PathVariable studentCourseDetails: StudentCourseDetails): IndexedSeq[CourseYearWeighting] = {
    (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
      courseAndRouteService.getCourseYearWeighting(mandatory(studentCourseDetails).course.code,
        mandatory(studentCourseDetails.sprStartAcademicYear), year)
    ).sorted
  }

  @RequestMapping()
  def viewStudentAssessmentDetails(
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear,
    @ModelAttribute("command") cmd: StudentAssessmentCommand.Command
  ): Mav = {

    val breakdown = cmd.apply()
    val assessmentComponents = breakdown.modules
    val passMarkMap = assessmentComponents.map(ac => {
      val module = ac.moduleRegistration.module
      module -> ProgressionService.modulePassMark(module.degreeType)
    }).toMap
    val normalLoadLookup: NormalLoadLookup = NormalLoadLookup(academicYear, cmd.studentCourseYearDetails.yearOfStudy, normalCATSLoadService)

    Mav("exams/grids/generate/studentAssessmentComponentDetails",
      "passMarkMap" -> passMarkMap,
      "assessmentComponents" -> assessmentComponents,
      "normalLoadLookup" -> normalLoadLookup,
      "member" -> studentCourseDetails.student,
      "mitigatingCircumstances" -> breakdown.mitigatingCircumstances.getOrElse(Nil)
    ).crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(mandatory(cmd.studentCourseYearDetails.enrolmentDepartment), mandatory(academicYear)))
      .secondCrumbs(scydBreadcrumbs(academicYear, studentCourseDetails)(scyd => Routes.exams.Grids.assessmentdetails(scyd)): _*)

  }
}
