package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.feedback._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}.zip"))
class DownloadSelectedFeedbackController extends CourseworkController {

	var feedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute
	def singleFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) = new AdminGetSingleFeedbackCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: AdminGetSingleFeedbackCommand, @PathVariable filename: String): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadSelectedFeedbackFileController extends CourseworkController {

	var feedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def singleFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) =
		new AdminGetSingleFeedbackFileCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: AdminGetSingleFeedbackFileCommand, @PathVariable filename: String): Mav = {
		val renderable = cmd.apply().getOrElse {
			throw new ItemNotFoundException()
		}
		Mav(new RenderableFileView(renderable))
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback.zip"))
class DownloadAllFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def selectedFeedbacksCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadSelectedFeedbackCommand(module, assignment, user)

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

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{feedbackId}/{filename}"))
class DownloadMarkerFeedbackController extends CourseworkController {

	var feedbackDao = Wire.auto[FeedbackDao]

	@RequestMapping
	def markerFeedback(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String,
		@PathVariable filename: String,
		@PathVariable marker: User
	) = {
		feedbackDao.getMarkerFeedback(feedbackId) match {
			case Some(markerFeedback) =>
				val renderable = new AdminGetSingleMarkerFeedbackCommand(module, assignment, markerFeedback).apply()
				Mav(new RenderableFileView(renderable))
			case None => throw new ItemNotFoundException
		}
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{feedbackId}/{filename}"))
class DownloadMarkerFeedbackControllerCurrentUser extends CourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, @PathVariable feedbackId: String, @PathVariable filename: String, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.marker(assignment, currentUser.apparentUser, feedbackId, filename))
	}
}

@Controller
class DownloadMarkerFeebackFilesController extends BaseController {

	@ModelAttribute def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable markerFeedback: String,
		@PathVariable marker: User
	) = new DownloadMarkerFeedbackFilesCommand(module, assignment, markerFeedback)

	// the difference between the RequestMapping paths for these two methods is a bit subtle - the first has
	// attachments plural, the second has attachments singular.
	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{markerFeedback}/attachments/*"))
	def getAll(command: DownloadMarkerFeedbackFilesCommand): Mav = {
		getOne(command, null)
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{markerFeedback}/attachments/*"))
	def redirectAll(@PathVariable assignment: Assignment, @PathVariable markerFeedback: String, @PathVariable marker: User): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.all(assignment, marker, markerFeedback))
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{markerFeedback}/attachment/{filename}"))
	def getOne(command: DownloadMarkerFeedbackFilesCommand, @PathVariable filename: String): Mav = {
		val renderable = command.apply().getOrElse {
			throw new ItemNotFoundException()
		}
		Mav(new RenderableFileView(renderable))
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{markerFeedback}/attachment/{filename}"))
	def redirectOne(@PathVariable assignment: Assignment, @PathVariable markerFeedback: String, @PathVariable marker: User, @PathVariable filename: String) {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.one(assignment, marker, markerFeedback, filename))
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/{position}/feedback.zip"))
class DownloadFirstMarkersFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def downloadFirstMarkersFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable position: String,
		@PathVariable marker: User,
		user: CurrentUser
	) = {
		val feedbackPosition = position match {
			case "firstmarker" => FirstFeedback
			case "secondmarker" => SecondFeedback
		}
		DownloadMarkersFeedbackForPositionCommand(module, assignment, marker, user, feedbackPosition)
	}

	@RequestMapping
	def getSelected(@ModelAttribute("command") command: Appliable[RenderableFile]): Mav = {
		Mav(new RenderableFileView(command.apply()))
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download-zip/{filename}"))
class DownloadAllFeedback extends CourseworkController {

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new AdminGetAllFeedbackCommand(module, assignment)

	@RequestMapping
	def download(cmd: AdminGetAllFeedbackCommand, @PathVariable filename: String): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}
}

// A read only view of all feedback fields and attachments
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/summary/{student}"))
class FeedbackSummaryController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable student: User)
	= FeedbackSummaryCommand(assignment, student)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: Appliable[Option[Feedback]]): Mav = {
		val feedback = command.apply()
		Mav("admin/assignments/feedback/read_only", "feedback" -> feedback).noLayout()
	}

}