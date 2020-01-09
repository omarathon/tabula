package uk.ac.warwick.tabula.web.controllers.exams

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.system.UserNavigationGeneratorImpl.moduleService
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/exams"))
class ExamsComponentHomeController extends ExamsController {

  @Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _
  @Autowired var assessmentService: AssessmentService = _
  @Autowired var features: Features = _

  @RequestMapping
  def home: Mav = {
    val homeDepartment = moduleAndDepartmentService.getDepartmentByCode(user.apparentUser.getDepartmentCode)
    // TODO - reuse this for the new exam management
    val examsEnabled = features.exams && user.isStaff && false
    val canDeptAdmin = user.loggedIn && moduleService.departmentsWithPermission(user, Permissions.Department.Reports).nonEmpty
    val examGridsEnabled = features.examGrids && user.isStaff && (canDeptAdmin || moduleService.departmentsWithPermission(user, Permissions.Department.ExamGrids).nonEmpty)

    if (!examsEnabled && examGridsEnabled) {
      Redirect(Routes.Grids.home)
    } else {
      Mav("exams/home",
        "examsEnabled" -> examsEnabled,
        "examGridsEnabled" -> examGridsEnabled
      ).secondCrumbs(
        (if (examGridsEnabled) Seq(ExamsBreadcrumbs.Grids.Home) else Nil): _*
      )
    }
  }

}
