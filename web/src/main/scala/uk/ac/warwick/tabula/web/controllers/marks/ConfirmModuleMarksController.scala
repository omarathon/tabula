package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.{ConfirmModuleMarksAction, ConfirmModuleMarksCommand, ListAssessmentComponentsCommand, MarksDepartmentHomeCommand}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Module}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

@Controller
@RequestMapping(Array("/marks/admin/module/{sitsModuleCode}/{academicYear}/{occurrence}/confirm"))
class ConfirmModuleMarksController extends BaseModuleMarksController
  with AutowiringProfileServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  @ModelAttribute("command")
  def command(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): ConfirmModuleMarksCommand.Command =
    ConfirmModuleMarksCommand(sitsModuleCode, module, academicYear, occurrence, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): Seq[BreadCrumb] = {
    val department = module.adminDepartment

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.ModuleOccurrenceConfirmMarks(sitsModuleCode, module, academicYear, occurrence, active = true),
    )
  }

  @ModelAttribute("assessmentComponents")
  def assessmentComponents(@Valid @ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[AssessmentComponent] =
    command.assessmentComponents.sortBy(_.sequence)

  @ModelAttribute("studentModuleRecords")
  def studentModuleRecords(@Valid @ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.studentModuleRecords.sortBy(_._1.sprCode)

  @ModelAttribute("alreadyConfirmed")
  def alreadyConfirmed(@Valid @ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.alreadyConfirmed.sortBy(_._1.sprCode)

  @ModelAttribute("studentsToConfirm")
  def studentsToConfirm(@Valid @ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Seq[(MarksDepartmentHomeCommand.StudentModuleMarkRecord, Map[AssessmentComponent, ListAssessmentComponentsCommand.StudentMarkRecord])] =
    command.studentsToConfirm.sortBy(_._1.sprCode)

  @ModelAttribute("validActions")
  def validActions(@Valid @ModelAttribute("command") command: ConfirmModuleMarksCommand.Command, errors: Errors): Map[String, Seq[ConfirmModuleMarksAction]] =
    command.studentsToConfirm.map(_._1.sprCode).map { sprCode =>
      val hasMissingModuleGrade = command.studentsWithMissingModuleGrade.exists(_._1.sprCode == sprCode)
      val hasMissingComponentGrade = command.studentsWithMissingComponentGrades.exists(_._1.sprCode == sprCode)

      sprCode -> {
        if (hasMissingModuleGrade || hasMissingComponentGrade) ConfirmModuleMarksAction.values.filterNot(_ == ConfirmModuleMarksAction.Confirm)
        else ConfirmModuleMarksAction.values
      }
    }.toMap

  private val formView: String = "marks/admin/modules/confirm"

  // We run validation when showing the form so we can avoid people clicking the button
  @RequestMapping(params = Array("!confirm"))
  def preview(@Valid @ModelAttribute("command") cmd: ConfirmModuleMarksCommand.Command, errors: Errors): String =
    formView

  @RequestMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: ConfirmModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("module") module: Module,
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
