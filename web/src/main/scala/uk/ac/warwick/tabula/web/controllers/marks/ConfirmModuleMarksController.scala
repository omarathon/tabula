package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.{CalculateModuleMarksCommand, ConfirmModuleMarksCommand, ListAssessmentComponentsCommand, MarksDepartmentHomeCommand}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Module}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

@Controller
@RequestMapping(Array("/marks/admin/module/{module}-{cats}/{academicYear}/{occurrence}/confirm"))
class ConfirmModuleMarksController extends BaseController
  with AutowiringProfileServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable module: Module, @PathVariable cats: BigDecimal, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): ConfirmModuleMarksCommand.Command =
    ConfirmModuleMarksCommand(module, cats, academicYear, occurrence, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable module: Module, @PathVariable cats: BigDecimal, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): Seq[BreadCrumb] = {
    val department = module.adminDepartment

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.ModuleOccurrenceConfirmMarks(module, cats, academicYear, occurrence, active = true),
    )
  }

  @ModelAttribute("assessmentComponents")
  def assessmentComponents(@ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[AssessmentComponent] =
    command.assessmentComponents.sortBy(_.sequence)

  @ModelAttribute("studentModuleRecords")
  def studentModuleRecords(@ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.studentModuleRecords.sortBy(_._1.sprCode)

  @ModelAttribute("alreadyConfirmed")
  def alreadyConfirmed(@ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.alreadyConfirmed.sortBy(_._1.sprCode)

  @ModelAttribute("studentsToConfirm")
  def studentsToConfirm(@ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.studentsToConfirm.sortBy(_._1.sprCode)

  private val formView: String = "marks/admin/modules/confirm"

  @RequestMapping(params = Array("!confirm"))
  def preview(
    @ModelAttribute("command") cmd: ConfirmModuleMarksCommand.Command,
    model: ModelMap,
    @PathVariable module: Module,
    @PathVariable cats: BigDecimal,
    @PathVariable academicYear: AcademicYear,
    @PathVariable occurrence: String
  ): String = formView

  @RequestMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: CalculateModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable module: Module,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()
      RedirectFlashing(Routes.marks.Admin.home(module.adminDepartment, academicYear), "flash__success" -> "flash.module.marksConfirmed")
    }

}
