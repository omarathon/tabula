package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.Member
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.commands.profiles.CreateMemberNoteCommand

@Controller
@RequestMapping(Array("/profiles/{member}/note/add"))
class CreateMemberNoteController extends BaseController {

	validatesSelf[CreateMemberNoteCommand]

	@ModelAttribute("command")
	def createCommand(@PathVariable member: Member) = new CreateMemberNoteCommand(member, user)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: CreateMemberNoteCommand) = {
	 Mav("profiles/membernote/form").noNavigation()
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: CreateMemberNoteCommand, errors: Errors) = {
	 if (errors.hasErrors) {
		 form(cmd)
	 } else {
		 cmd.apply()
		 Mav("profiles/membernote/form", "memberNoteSuccess" -> true).noNavigation()
	 }
	}

 }
