package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.profiles.commands.DeleteMemberNoteCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}

@Controller
@RequestMapping(value = Array("/{member}/note/{memberNote}/delete"))
class DeleteMemberNoteController extends BaseController {

	@ModelAttribute("command")
	def deleteCommand(@PathVariable member: Member, @PathVariable memberNote: MemberNote) = new DeleteMemberNoteCommand(memberNote, member, user)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMemberNoteCommand) = Mav("membernote/delete_form").noLayoutIf(ajax)

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMemberNoteCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("membernote/delete_success").noLayoutIf(ajax)
		}
	}

}
