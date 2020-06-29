package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.{GraduationBenchmarkBreakdownCommand, GraduationBenchmarkBreakdownCommandRequest}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.AutowiringNormalCATSLoadServiceComponent
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.{ExamsController, StudentCourseYearDetailsBreadcrumbs}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}


@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/{studentCourseDetails}/benchmarkdetails"))
class GraduationBenchmarkBreakdownController extends ExamsController
  with DepartmentScopedController with AcademicYearScopedController with StudentCourseYearDetailsBreadcrumbs
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
  def command(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear): GraduationBenchmarkBreakdownCommand.Command = {
    GraduationBenchmarkBreakdownCommand(mandatory(studentCourseDetails), mandatory(academicYear))
  }

  @RequestMapping()
  def viewBenchmarkDetails(
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear,
    @ModelAttribute("command") cmd: GraduationBenchmarkBreakdownCommand.Command
  ): Mav = {
    if(!features.graduationBenchmark) throw new ItemNotFoundException() // 404 if the feature is off

    val mav = cmd.apply() match {
      case Left(ugBreakdown) =>
        Mav("exams/grids/generate/graduationBenchmarkDetails",
          "breakdown" -> ugBreakdown,
          "member" -> studentCourseDetails.student,
        )
      case Right(pgBreakdown) =>
        Mav("exams/grids/generate/pgGraduationBenchmarkDetails",
          "breakdown" -> pgBreakdown,
          "member" -> studentCourseDetails.student,
        )
    }

    mav.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(mandatory(cmd.studentCourseYearDetails.enrolmentDepartment), mandatory(academicYear)))
      .secondCrumbs(scydBreadcrumbs(academicYear, studentCourseDetails)(scyd => Routes.exams.Grids.benchmarkdetails(scyd, cmd.calculateYearMarks, cmd.groupByLevel)): _*)
  }

}
