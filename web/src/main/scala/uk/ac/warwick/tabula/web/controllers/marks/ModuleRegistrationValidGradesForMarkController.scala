package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.ModuleRegistrationValidGradesForMarkCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/marks/admin/module/{module}-{cats}/{academicYear}/{occurrence}/generate-grades"), params = Array("sprCode"))
class ModuleRegistrationValidGradesForMarkController
  extends BaseController
    with AutowiringModuleRegistrationServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable module: Module, @PathVariable cats: BigDecimal, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String, @RequestParam sprCode: String): ModuleRegistrationValidGradesForMarkCommand.Command = {
    val moduleRegistration = mandatory {
      moduleRegistrationService.getByModuleOccurrence(module, cats, academicYear, occurrence)
        .find(_.sprCode == sprCode)
    }

    ModuleRegistrationValidGradesForMarkCommand(moduleRegistration)
  }

  @PostMapping
  def validGrades(@Valid @ModelAttribute("command") command: ModuleRegistrationValidGradesForMarkCommand.Command, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav("_generatedGrades",
        "grades" -> Seq(),
        "default" -> null
      ).noLayout()
    } else {
      val (grades, default) = command.apply()
      Mav("_generatedGrades",
        "grades" -> grades,
        "default" -> default
      ).noLayout()
    }
  }

}
