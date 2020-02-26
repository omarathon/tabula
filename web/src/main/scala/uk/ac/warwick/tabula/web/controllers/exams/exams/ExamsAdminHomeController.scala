package uk.ac.warwick.tabula.web.controllers.exams.exams


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

abstract class AbstractExamsAdminHomeController
  extends ExamsController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  override val departmentPermission: Permission = Permissions.Exam.Read

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  @GetMapping
  def home(@PathVariable department: Department/*, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear] = None*/): Mav = {
    val academicYear = activeAcademicYear.getOrElse(AcademicYear.now())
    Mav("exams/admin/department")
      .crumbsList(Breadcrumbs.Exams.department(department, activeAcademicYear))
      //.secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.exams.Exams.admin.department(department, _)): _*)
  }
}

@Controller
@RequestMapping(Array("/exams/admin/{department}"))
class ExamsAdminHomeController extends AbstractExamsAdminHomeController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/exams/admin/{department}/{academicYear:\\d{4}}"))
class ExamsAdminHomeForYearController extends AbstractExamsAdminHomeController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))
}
