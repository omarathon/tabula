package uk.ac.warwick.tabula.groups.web.controllers.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.reusable.AllocateStudentsToDepartmentalSmallGroupsTemplateCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value=Array("/admin/department/{department}/groups/reusable/{smallGroupSet}/template"))
class AllocateStudentsToDepartmentalSmallGroupsTemplateController extends BaseController {

	type AllocateStudentsToDepartmentalSmallGroupsTemplateCommand = Appliable[ExcelView]

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): AllocateStudentsToDepartmentalSmallGroupsTemplateCommand =
		AllocateStudentsToDepartmentalSmallGroupsTemplateCommand(department, set)

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: AllocateStudentsToDepartmentalSmallGroupsTemplateCommand) = {
		cmd.apply()
	}

}
