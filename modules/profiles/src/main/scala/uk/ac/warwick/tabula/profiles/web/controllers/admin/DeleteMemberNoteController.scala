package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.profiles.commands.DeleteMemberNoteCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}

@Controller
@RequestMapping(value = Array("/{member}/note/{note}/delete"))
class DeleteMemberNoteController extends BaseController {

	@ModelAttribute("command")
	def deleteCommand(@PathVariable member: Member, @PathVariable note: MemberNote) = new DeleteMemberNoteCommand(note, member, user)

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMemberNoteCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			cmd.apply()
			Mav(new JSONView(Map("status" -> "successful")))
		}
	}

}

