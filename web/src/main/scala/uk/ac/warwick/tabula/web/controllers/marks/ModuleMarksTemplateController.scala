package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.ModuleMarksTemplateCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/marks/admin/module/{module}-{cats}/{academicYear}/{occurrence}/marks/template.xlsx"))
class ModuleMarksTemplateController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable module: Module, @PathVariable cats: BigDecimal, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): ModuleMarksTemplateCommand.Command =
    ModuleMarksTemplateCommand(module, cats, academicYear, occurrence)

  @RequestMapping
  def template(@ModelAttribute("command") command: ModuleMarksTemplateCommand.Command): ExcelView =
    command.apply()

}
