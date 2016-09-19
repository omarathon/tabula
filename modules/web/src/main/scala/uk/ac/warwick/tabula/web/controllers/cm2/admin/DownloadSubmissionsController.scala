package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissions.zip"))
class DownloadSubmissionsController extends CourseworkController {

	@ModelAttribute("command")
	def getSingleSubmissionCommand(@PathVariable assignment: Assignment) =
		new DownloadSubmissionsCommand(assignment.module, assignment, user)

	@RequestMapping
	def download(@ModelAttribute("command") command: DownloadSubmissionsCommand, @PathVariable assignment: Assignment): Mav = {
		command.apply() match {
			case Left(renderable) =>
				Mav(new RenderableFileView(renderable))
			case Right(jobInstance) =>
				Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/submissions.zip"))
class DownloadMarkerSubmissionsController extends CourseworkController {

	@ModelAttribute("command")
	def getMarkersSubmissionCommand(
			@PathVariable assignment: Assignment,
			@PathVariable marker: User,
			submitter: CurrentUser
	) =	DownloadMarkersSubmissionsCommand(assignment.module, assignment, marker, submitter)

	@RequestMapping
	def downloadMarkersSubmissions(@ModelAttribute("command") command: Appliable[RenderableFile]): RenderableFile = {
		command.apply()
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/submissions.zip"))
class DownloadMarkerSubmissionsControllerCurrentUser extends CourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.submissions(assignment, currentUser.apparentUser))
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissions/download-zip/{filename}"))
class DownloadAllSubmissionsController extends CourseworkController {

	@ModelAttribute def getAllSubmissionsSubmissionCommand(
			@PathVariable assignment: Assignment,
			@PathVariable filename: String) =
		new DownloadAllSubmissionsCommand(assignment.module, assignment, filename)

	@RequestMapping
	def downloadAll(command: DownloadAllSubmissionsCommand): RenderableFile = {
		command.apply()
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(
	value = Array("/${cm2.prefix}/admin/assignments/{assignment}/submissions/download/{submission}/{filename}.zip"),
	params = Array("!single"))
class DownloadSingleSubmissionController extends CourseworkController {

	var userLookup = Wire[UserLookupService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission) =
		new AdminGetSingleSubmissionCommand(assignment.module, assignment, mandatory(submission))

	@RequestMapping
	def downloadSingle(cmd: AdminGetSingleSubmissionCommand): RenderableFile = cmd.apply()
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissions/download/{submission}/{filename}"))
class DownloadSingleSubmissionFileController extends CourseworkController {
	var userLookup = Wire[UserLookupService]
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission ) = {
		val student = profileService.getMemberByUser(userLookup.getUserByUserId(mandatory(submission).userId))
		new DownloadAttachmentCommand(assignment.module, assignment, mandatory(submission), student)
	}

	@RequestMapping
	def downloadSingle(cmd: DownloadAttachmentCommand): RenderableFile = {
		val file = cmd.apply()
		file.getOrElse { throw new ItemNotFoundException() }
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class DownloadFeedbackSheetsController extends CourseworkController {

	var userLookup = Wire.auto[UserLookupService]

	@ModelAttribute def feedbackSheetsCommand(@PathVariable assignment: Assignment) =
		new DownloadFeedbackSheetsCommand(assignment.module, assignment)

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
