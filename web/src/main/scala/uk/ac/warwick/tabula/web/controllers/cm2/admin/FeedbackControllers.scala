package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.feedback._
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkerFeedback}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

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


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback.zip"))
class DownloadAllFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def selectedFeedbacksCommand(@PathVariable assignment: Assignment) =
		new DownloadSelectedFeedbackCommand(mandatory(assignment), user)

	@RequestMapping
	def getSelected(@ModelAttribute("command") command: DownloadSelectedFeedbackCommand, @PathVariable assignment: Assignment): Mav = {
		command.apply() match {
			case Left(renderable) =>
				Mav(new RenderableFileView(renderable))
			case Right(jobInstance) =>
				Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/download/{feedbackId}/{filename}.zip"))
class DownloadSelectedFeedbackController extends CourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute
	def singleFeedbackCommand(
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) = new AdminGetSingleFeedbackCommand(mandatory(assignment), mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(GET))
	def get(cmd: AdminGetSingleFeedbackCommand, @PathVariable filename: String): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadSelectedFeedbackFileController extends CourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def singleFeedbackCommand(
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) = new AdminGetSingleFeedbackFileCommand(mandatory(assignment), mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(GET))
	def get(cmd: AdminGetSingleFeedbackFileCommand, @PathVariable filename: String): Mav = {
		val renderable = cmd.apply().getOrElse {
			throw new ItemNotFoundException()
		}
		Mav(new RenderableFileView(renderable))
	}
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/summary/{student}"))
class FeedbackSummaryController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable student: User): FeedbackSummaryCommand.Command =
		FeedbackSummaryCommand(assignment, student)

	@RequestMapping
	def showFeedback(@ModelAttribute("command") command: FeedbackSummaryCommand.Command): Mav = {
		val feedback = command.apply()
		Mav("cm2/admin/assignments/feedback/read_only", "feedback" -> feedback).noLayout()
	}

}