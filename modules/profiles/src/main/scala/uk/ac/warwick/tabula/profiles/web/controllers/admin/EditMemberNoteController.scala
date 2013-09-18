package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{MemberNote, Member}
import uk.ac.warwick.tabula.profiles.commands.EditMemberNoteCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid
import org.springframework.validation.Errors

@Controller
@RequestMapping(value = Array("/{member}/note/{memberNote}/edit"))
class EditMemberNoteController extends BaseController {

	@ModelAttribute("command")
	def editCommand(@PathVariable member: Member, @PathVariable memberNote: MemberNote) = new EditMemberNoteCommand(memberNote, user)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: EditMemberNoteCommand) = {
		Mav("membernote/edit_form").noLayoutIf(ajax)

	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: EditMemberNoteCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("membernote/edit_success").noLayoutIf(ajax)
		}
	}

}
