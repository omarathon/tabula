package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{MemberNote, Member}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.profiles.RestoreMemberNoteCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

@Controller
@RequestMapping(value = Array("/profiles/{member}/note/{note}/restore"))
class RestoreMemberNoteController extends BaseController {

	@ModelAttribute("command")
	def restoreCommand(@PathVariable member: Member, @PathVariable note: MemberNote) = new RestoreMemberNoteCommand(note, member, user)

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: RestoreMemberNoteCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			cmd.apply()
			Mav(new JSONView(Map("status" -> "successful")))
		}
	}

}
