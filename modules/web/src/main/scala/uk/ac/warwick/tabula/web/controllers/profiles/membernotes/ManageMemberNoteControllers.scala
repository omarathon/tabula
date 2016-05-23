package uk.ac.warwick.tabula.web.controllers.profiles.membernotes

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.membernotes.{CreateExtenuatingCircumstancesCommand, CreateMemberNoteCommand, EditExtenuatingCircumstancesCommand, EditMemberNoteCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances, Member, MemberNote}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

abstract class AbstractManageMemberNoteController extends ProfilesController {

	validatesSelf[SelfValidating]

	protected def viewPrefix: String
	protected def isEdit: Boolean

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[AbstractMemberNote]) = {
		Mav(s"profiles/membernote/${viewPrefix}_form",
			"edit" -> isEdit
		).noNavigation()
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[AbstractMemberNote], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav(s"profiles/membernote/${viewPrefix}_form", "memberNoteSuccess" -> true).noNavigation()
		}
	}

}

@Controller
@RequestMapping(Array("/profiles/{member}/note/add"))
class CreateMemberNoteController extends AbstractManageMemberNoteController {

	@ModelAttribute("command")
	def createCommand(@PathVariable member: Member) =
		CreateMemberNoteCommand(member, user)

	override protected val viewPrefix: String = "membernote"

	override protected val isEdit: Boolean = false
}

@Controller
@RequestMapping(Array("/profiles/{member}/circumstances/add"))
class CreateExtenuatingCircumstancesController extends AbstractManageMemberNoteController {

	@ModelAttribute("command")
	def createCommand(@PathVariable member: Member) =
		CreateExtenuatingCircumstancesCommand(member, user)

	override protected val viewPrefix: String = "circumstances"

	override protected val isEdit: Boolean = false
}

@Controller
@RequestMapping(value = Array("/profiles/{member}/note/{memberNote}/edit"))
class EditMemberNoteController extends AbstractManageMemberNoteController {

	@ModelAttribute("command")
	def editCommand(@PathVariable member: Member, @PathVariable memberNote: MemberNote) =
		EditMemberNoteCommand(memberNote)

	override protected val viewPrefix: String = "membernote"

	override protected val isEdit: Boolean = false
}

@Controller
@RequestMapping(value = Array("/profiles/{member}/circumstances/{circumstances}/edit"))
class EditExtenuatingCircumstancesController extends AbstractManageMemberNoteController {

	@ModelAttribute("command")
	def editCommand(@PathVariable member: Member, @PathVariable circumstances: ExtenuatingCircumstances) =
		EditExtenuatingCircumstancesCommand(circumstances)

	override protected val viewPrefix: String = "circumstances"

	override protected val isEdit: Boolean = false
}

