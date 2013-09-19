package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.{MemberNote, Member}
import uk.ac.warwick.tabula.profiles.commands.PurgeMemberNoteCommand
import org.springframework.validation.Errors
import javax.validation.Valid

@Controller
@RequestMapping(value = Array("/{member}/note{note}/purge"))
class PurgeMemberNoteController extends BaseController {

@ModelAttribute("command")
def restoreCommand(@PathVariable member: Member, @PathVariable note: MemberNote) = new PurgeMemberNoteCommand(note, member, user)

@RequestMapping(method=Array(GET, HEAD))
def form(@ModelAttribute("command") cmd: PurgeMemberNoteCommand) = Mav("membernote/purge_form").noLayoutIf(ajax)

@RequestMapping(method=Array(POST))
def submit(@Valid @ModelAttribute("command") cmd: PurgeMemberNoteCommand, errors: Errors) = {
	if (errors.hasErrors) {
		form(cmd)
	} else {
		cmd.apply()
		Mav("membernote/purge_success").noLayoutIf(ajax)
	}
}

}
