package uk.ac.warwick.tabula.web.controllers.cm2.admin.marksmanagement

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.marksmanagement.MarksOpenAndCloseDepartmentsCommand
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Controller
@RequestMapping(value = Array("/coursework/admin/marksmanagement/departments"))
class OpenAndCloseDepartmentsController extends CourseworkController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command: MarksOpenAndCloseDepartmentsCommand.Command = MarksOpenAndCloseDepartmentsCommand()

  val formView: String = "cm2/admin/marksmanagement/open_close_departments"

  @ModelAttribute("currentYear")
  def currentYear: AcademicYear = AcademicYear.now()

  @ModelAttribute("previousYear")
  def previousYear(@ModelAttribute("currentYear") currentYear: AcademicYear): AcademicYear = currentYear - 1

  @RequestMapping
  def showForm(@ModelAttribute("command") cmd: MarksOpenAndCloseDepartmentsCommand.Command): String = {
    cmd.populate()
    formView
  }

  @PostMapping
  def submit(
    @Valid @ModelAttribute("command") cmd: MarksOpenAndCloseDepartmentsCommand.Command,
    errors: Errors,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()
      RedirectFlashing(Routes.cm2.openCloseDepartments, "flash__success" -> "flash.openCloseDepartments.saved")
    }
}
