package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.feedback.{DownloadMarkerFeedbackCommand, DownloadMarkerFeedbackState}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/feedback/download/{markerFeedback}"))
class DownloadMarkerFeedbackController extends CourseworkController {

	type Command = Appliable[Option[RenderableFile]] with DownloadMarkerFeedbackState

	@ModelAttribute def command(
		@PathVariable(value = "assignment") assignment: Assignment,
		@PathVariable(value = "markerFeedback") markerFeedback: MarkerFeedback
	): Command = DownloadMarkerFeedbackCommand(mandatory(assignment), mandatory(markerFeedback))

	@RequestMapping(value=Array("/attachments/*"))
	def getAll(@ModelAttribute command: Command with DownloadMarkerFeedbackState): Mav = {
		getOne(command, null)
	}

	@RequestMapping(value=Array("/attachment/{filename}"))
	def getOne(@ModelAttribute command: Command, @PathVariable filename: String): Mav = {
		val file = command.apply().getOrElse(throw new ItemNotFoundException())
		Mav(new RenderableFileView(file))
	}
}
