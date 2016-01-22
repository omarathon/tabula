package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.commands.coursework.assignments.{DownloadFeedbackSheetsCommand, DownloadAllSubmissionsCommand, DownloadSubmissionsCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableFile, RenderableZip}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.{Submission, Module, Assignment}
import uk.ac.warwick.tabula.commands.coursework.assignments.AdminGetSingleSubmissionCommand
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadMarkersSubmissionsCommand
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadAttachmentCommand
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.userlookup.User


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
class DownloadSubmissionsController extends CourseworkController {

	@ModelAttribute("command")
	def getSingleSubmissionCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadSubmissionsCommand(module, assignment, user)

	@RequestMapping
	def download(@ModelAttribute("command") command: DownloadSubmissionsCommand, @PathVariable assignment: Assignment)
		(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
		command.apply() match {
			case Left(renderable) =>
				Mav(new RenderableFileView(renderable))
			case Right(jobInstance) =>
				Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/submissions.zip"))
class DownloadMarkerSubmissionsController extends CourseworkController {

	@ModelAttribute("command")
	def getMarkersSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable marker: User,
			submitter: CurrentUser
	) =	DownloadMarkersSubmissionsCommand(module, assignment, marker, submitter)

	@RequestMapping
	def downloadMarkersSubmissions(@ModelAttribute("command") command: Appliable[RenderableZip]): RenderableFile = {
		command.apply()
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/submissions.zip"))
class DownloadMarkerSubmissionsControllerCurrentUser extends CourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.submissions(assignment, currentUser.apparentUser))
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
class DownloadAllSubmissionsController extends CourseworkController {

	@ModelAttribute def getAllSubmissionsSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable filename: String) =
		new DownloadAllSubmissionsCommand(module, assignment, filename)

	@RequestMapping
	def downloadAll(command: DownloadAllSubmissionsCommand): RenderableFile = {
		command.apply()
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download/{submission}/{filename}.zip"))
class DownloadSingleSubmissionController extends CourseworkController {

	var userLookup = Wire[UserLookupService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission) =
		new AdminGetSingleSubmissionCommand(module, assignment, mandatory(submission))

	@RequestMapping
	def downloadSingle(cmd: AdminGetSingleSubmissionCommand): RenderableFile = cmd.apply()
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download/{submission}/{filename}"))
class DownloadSingleSubmissionFileController extends CourseworkController {
	var userLookup = Wire[UserLookupService]
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission ) = {
		val student = profileService.getMemberByUser(userLookup.getUserByUserId(mandatory(submission).userId))
		new DownloadAttachmentCommand(module, assignment, mandatory(submission), student)
	}

	@RequestMapping
	def downloadSingle(cmd: DownloadAttachmentCommand): RenderableFile = {
		val file = cmd.apply()
		file.getOrElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}"))
class DownloadFeedbackSheetsController extends CourseworkController {

	var userLookup = Wire.auto[UserLookupService]

	@ModelAttribute def feedbackSheetsCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadFeedbackSheetsCommand(module, assignment)

	@RequestMapping(value = Array("/feedback-templates.zip"))
	def downloadFeedbackTemplatesOnly(command: DownloadFeedbackSheetsCommand): RenderableFile = {
		command.apply()
	}

	@RequestMapping(value = Array("/marker-templates.zip"))
	def downloadMarkerFeedbackTemplates(command: DownloadFeedbackSheetsCommand): RenderableFile = {
		val assignment = command.assignment

		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val users = submissions.map(s => userLookup.getUserByUserId(s.userId))
		command.members = users
		command.apply()
	}

}
