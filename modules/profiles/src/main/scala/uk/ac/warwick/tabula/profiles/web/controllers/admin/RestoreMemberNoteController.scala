package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{MemberNote, Member}
import scala.Array
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.profiles.commands.RestoreMemberNoteCommand
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/{member}/note{note}/restore"))
class RestoreMemberNoteController extends BaseController {

	@ModelAttribute("command")
	def restoreCommand(@PathVariable member: Member, @PathVariable note: MemberNote) = new RestoreMemberNoteCommand(note, member, user)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: RestoreMemberNoteCommand) = Mav("membernote/restore_form").noLayoutIf(ajax)

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: RestoreMemberNoteCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("membernote/restore_success").noLayoutIf(ajax)
		}
	}

}
