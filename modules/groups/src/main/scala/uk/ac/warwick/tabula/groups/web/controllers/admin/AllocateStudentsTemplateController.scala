package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.AllocateStudentsTemplateCommand
import javax.validation.Valid


@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/{set}/allocate/template"))
class AllocateStudentsTemplateController extends BaseController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable set: SmallGroupSet) = new AllocateStudentsTemplateCommand(module, set, user)

	@RequestMapping
	def getTemplate(@Valid cmd:AllocateStudentsTemplateCommand) = {
		cmd.apply()
	}

}
