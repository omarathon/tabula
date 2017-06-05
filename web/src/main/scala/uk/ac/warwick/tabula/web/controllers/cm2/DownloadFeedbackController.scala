package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.cm2.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}"))
class DownloadFeedbackController extends CourseworkController {

	var feedbackService: FeedbackService = Wire[FeedbackService]

	@ModelAttribute("downloadFeedbackCommand")
	def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		DownloadFeedbackCommand(assignment, mandatory(feedbackService.getAssignmentFeedbackByUsercode(assignment, user.userId).filter(_.released)))

	@RequestMapping(value = Array("/all/feedback.zip"))
	def getAll(@ModelAttribute("downloadFeedbackCommand") command: DownloadFeedbackCommand.Command): RenderableFile =
		getOne(command)

	@RequestMapping(value = Array("/get/{filename}"))
	def getOne(@ModelAttribute("downloadFeedbackCommand") command: DownloadFeedbackCommand.Command): RenderableFile =
		command.apply().getOrElse { throw new ItemNotFoundException() }

}
