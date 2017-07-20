package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.{DownloadAdminSubmissionsForPrintingCommand, DownloadMarkerSubmissionsForPrintingCommand, DownloadSubmissionsForPrintingCommand}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

trait DownloadSubmissionsForPrintingController extends CourseworkController with AutowiringUserLookupComponent {

	@RequestMapping
	def pdfCheck(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand.Command, @PathVariable assignment: Assignment): Mav = {
		Mav(new JSONView(Map(
			"submissionsWithNonPDFs" -> cmd.submissions.filter(
				_.allAttachments.exists(!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension))
			).map(submission =>
				Map(
					"submission" -> submission.id,
					"universityId" -> submission.studentIdentifier,
					"name" -> (if (assignment.module.adminDepartment.showStudentName) userLookup.getUserByUserId(submission.usercode).getFullName else ""),
					"nonPDFFiles" -> submission.allAttachments.filter(
						!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension)
					).map(_.name)
				)
			)
		))).noLayout()
	}

	@RequestMapping(params = Array("download"))
	def download(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand.Command): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/submissions.pdf"))
class DownloadAdminSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): DownloadSubmissionsForPrintingCommand.Command =
		DownloadAdminSubmissionsForPrintingCommand(assignment)

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/submissions.pdf"))
class DownloadMarkerSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, submitter: CurrentUser): DownloadSubmissionsForPrintingCommand.Command =
		DownloadMarkerSubmissionsForPrintingCommand(assignment, marker, submitter)

}