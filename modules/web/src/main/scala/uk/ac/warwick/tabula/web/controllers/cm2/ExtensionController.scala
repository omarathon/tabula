package uk.ac.warwick.tabula.web.controllers.cm2

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions._
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions"))
class FilterExtensionsController extends CourseworkController {

	type FilterExtensionsCommand = Appliable[FilterExtensionResults] with FilterExtensionsState

	@ModelAttribute("filterExtensionsCommand")
	def filterCommand() = FilterExtensionsCommand(user)

	@RequestMapping(method=Array(HEAD,GET))
	def viewForm(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
		val results = cmd.apply()
		Mav(s"$urlPrefix/admin/extensions/list",
			"command" -> cmd,
			"results" -> results
		)
	}

	@RequestMapping(method=Array(POST))
	def listFilterResults(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
		val results = cmd.apply()
		Mav(s"$urlPrefix/admin/extensions/_filter_results",
			"command" -> cmd,
			"results" -> results
		).noLayout()
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions/{extension}"))
class ExtensionController extends CourseworkController {

	type ExtensionsDetailCommand = Appliable[ExtensionDetail] with ViewExtensionState
	type ModifyExtensionCommand = Appliable[Extension] with ModifyExtensionState


	validatesSelf[SelfValidating]

	@ModelAttribute("extensionDetailCommand")
	def detailCommand(@PathVariable extension: Extension) = ViewExtensionCommand(extension)

	@ModelAttribute("modifyExtensionCommand")
	def modifyCommand(@PathVariable extension: Extension) = ModifyExtensionCommand(extension)

	@RequestMapping(method=Array(GET), path=Array("detail"))
	def detail(
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
		errors: Errors
	): Mav = {
		val detail = detailCommand.apply()
		Mav(s"$urlPrefix/admin/extensions/detail",
			"detail" -> detail,
			"modifyExtensionCommand" -> updateCommand,
			"states" -> ExtensionState
		).noLayout()
	}

	@RequestMapping(method=Array(POST), path=Array("update"))
	def update(
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@Valid @ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
		result: BindingResult,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			detail(detailCommand, updateCommand, errors)
		} else {
			updateCommand.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"redirect" -> Routes.admin.extensions()
			)))
		}
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions/{extension}/supporting-file/{filename}"))
class DownloadExtensionAttachmentController extends CourseworkController {

	type DownloadAttachmentCommand = Appliable[Option[RenderableAttachment]] with ModifyExtensionState

	@ModelAttribute("downloadAttachmentCommand")
	def attachmentCommand(@PathVariable extension: Extension, @PathVariable filename: String) =
		DownloadExtensionAttachmentCommand(extension, filename)

	@RequestMapping(method=Array(GET))
	def supportingFile(
		@ModelAttribute("downloadAttachmentCommand") attachmentCommand: DownloadAttachmentCommand,
		@PathVariable("filename") filename: String
	): RenderableFile = {
		attachmentCommand.apply().getOrElse{ throw new ItemNotFoundException() }
	}
}