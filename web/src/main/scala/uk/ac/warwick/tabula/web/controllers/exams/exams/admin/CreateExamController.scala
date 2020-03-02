package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import play.api.libs.json.{JsValue, _}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.exams.exams.admin.CreateExamCommand
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Department, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/exams/admin/{department}/{academicYear}/{module}/create"))
class CreateExamController extends ExamsController
  with DepartmentScopedController
  with AcademicYearScopedController
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringUserSettingsServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  validatesSelf[SelfValidating]

  override val departmentPermission: Permission = CreateExamCommand.RequiredPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = Some(academicYear)

  @ModelAttribute("command")
  def command(@PathVariable module: Module, @PathVariable academicYear: AcademicYear, user: CurrentUser): CreateExamCommand.Command = {
    CreateExamCommand(module, academicYear)
  }

  @RequestMapping
  def form(
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable module: Module,
    @ModelAttribute("command") command: CreateExamCommand.Command,
    errors: Errors
  ): Mav = {
    mustBeLinked(mandatory(module), mandatory(department))

    if (!errors.hasErrors) command.populate()

    val students: Map[AssessmentComponent, JsValue] = command.assessmentGroups.view.mapValues { groups =>
      Json.toJson(groups.map { group =>
        group.upstreamAssessmentGroup.occurrence -> group.currentMembers.map { m =>
          val user = command.students.get(m.universityId)
          Map(
            "universityId" -> m.universityId,
            "firstName" -> user.map(_.getFirstName).getOrElse("Unknown"),
            "lastName" -> user.map(_.getLastName).getOrElse("Unknown"),
            "usercode" -> user.map(_.getUserId).getOrElse("Unknown"),
            "seatNumber" -> m.position.map(_.toString).getOrElse(""),
          )
        }
      }.toMap)
    }.toMap

    Mav("exams/admin/exam/create",
      "availableComponents" -> command.availableComponents,
      "availableOccurrences" -> command.availableOccurrences,
      "students" -> students
    )
      .crumbsList(Breadcrumbs.Exams.module(module, academicYear))
  }

  @PostMapping
  def create(
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @PathVariable module: Module,
    @Valid @ModelAttribute("command") command: CreateExamCommand.Command,
    errors: Errors
  ) : Mav = {
    if (errors.hasErrors) form(department, academicYear, module, command, errors)
    else {
      command.apply()
      RedirectForce(Routes.Exams.admin.department(department))
    }
  }


}
