package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.ProcessModuleMarksCommand
import uk.ac.warwick.tabula.data.model.{MarkState, Module, ModuleResult}
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/marks/admin/module/{sitsModuleCode}/{academicYear}/{occurrence}/process"))
class ProcessModuleMarksController extends BaseModuleMarksController
  with StudentModuleMarkRecordNotificationDepartment {

  @ModelAttribute("command")
  def command(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): ProcessModuleMarksCommand.Command =
    ProcessModuleMarksCommand(sitsModuleCode, module, academicYear, occurrence, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): Seq[BreadCrumb] = {
    val department = module.adminDepartment

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.ModuleOccurrenceRecordMarks(sitsModuleCode, module, academicYear, occurrence, active = true),
    )
  }

  @ModelAttribute("studentModuleMarkRecords")
  def studentModuleMarkRecords(@ModelAttribute("command") command: ProcessModuleMarksCommand.Command): Seq[StudentModuleMarkRecord] =
    command.studentModuleMarkRecords

  @ModelAttribute("moduleResults")
  def moduleResults(): Seq[ModuleResult] = ModuleResult.values

  private val formView: String = "marks/admin/modules/process"

  @RequestMapping
  def showForm(@ModelAttribute("command") command: ProcessModuleMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping(params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("command") cmd: ProcessModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[StudentModuleMarkRecord],
    @PathVariable sitsModuleCode: String,
    @PathVariable academicYear: AcademicYear,
    @PathVariable occurrence: String
  ): String =
    if (errors.hasErrors) {
      if (errors.hasFieldErrors("file")) {
        model.addAttribute("from_origin", "upload")
      } else {
        model.addAttribute("from_origin", "webform")
      }
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      // Filter down to just changes
      val changes: Seq[(StudentModuleMarkRecord, Boolean)] =
        cmd.students.asScala.values.toSeq.filter(_.process).flatMap { student =>
          // We know the .get is safe because it's validated
          val studentModuleMarkRecord = studentModuleMarkRecords.find(_.sprCode == student.sprCode).get

          val isNotNotifiableChange =
            !student.comments.hasText &&
            ((!student.mark.hasText && studentModuleMarkRecord.mark.isEmpty) || studentModuleMarkRecord.mark.map(_.toString).contains(student.mark)) &&
            ((!student.grade.hasText && studentModuleMarkRecord.grade.isEmpty) || studentModuleMarkRecord.grade.contains(student.grade)) &&
            ((!student.result.hasText && studentModuleMarkRecord.result.isEmpty) || studentModuleMarkRecord.result.map(_.dbValue).contains(student.result))

          // Mark and grade and result haven't changed and no comment and not out of sync (we always re-push out of sync records)
          if (
            !studentModuleMarkRecord.outOfSync &&
            (studentModuleMarkRecord.markState.contains(MarkState.Agreed) || studentModuleMarkRecord.agreed) &&
            isNotNotifiableChange
          ) None else Some(studentModuleMarkRecord -> !isNotNotifiableChange)
        }

      model.addAttribute("changes", changes.map(_._1))
      model.addAttribute("notificationDepartments", departmentalStudents(changes.filter(_._2).map(_._1)))
      model.addAttribute("returnTo", getReturnTo(Routes.marks.Admin.ModuleOccurrences.processMarks(sitsModuleCode, academicYear, occurrence)))

      "marks/admin/modules/process_preview"
    }

  @PostMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: ProcessModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("module") module: Module,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("from_origin", "webform")
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()

      RedirectFlashing(Routes.marks.Admin.home(module.adminDepartment, academicYear), "flash__success" -> "flash.module.marksProcessed")
    }

}
