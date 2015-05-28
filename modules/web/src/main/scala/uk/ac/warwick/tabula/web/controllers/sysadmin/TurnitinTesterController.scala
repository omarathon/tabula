package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.sysadmin.TurnitinInitialTesterCommand
import org.springframework.web.bind.annotation
import javax.validation.Valid
import uk.ac.warwick.tabula.commands.Appliable
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser

@Controller
@RequestMapping(value = Array("/sysadmin/turnitin-tester/test"))
class TurnitinInitialTesterController extends BaseSysadminController {
	@ModelAttribute("turnitinInitialTesterCommand")
	def turnitinInitialTesterCommand(user: CurrentUser) = TurnitinInitialTesterCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitin-tester/test")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("turnitinInitialTesterCommand") cmd: Appliable[Boolean], errors: Errors) =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/turnitin-tester")
		}
}