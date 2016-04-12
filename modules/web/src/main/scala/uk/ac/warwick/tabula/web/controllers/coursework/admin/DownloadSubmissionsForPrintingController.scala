package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{DownloadAdminSubmissionsForPrintingCommand, DownloadMarkerSubmissionsForPrintingCommand, DownloadSubmissionsForPrintingCommandRequest}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.coursework.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

trait DownloadSubmissionsForPrintingController extends CourseworkController with AutowiringUserLookupComponent {

	type DownloadSubmissionsForPrintingCommand = Appliable[RenderableFile] with DownloadSubmissionsForPrintingCommandRequest

	@RequestMapping
	def pdfCheck(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand, @PathVariable module: Module): Mav = {
		Mav(new JSONView(Map(
			"submissionsWithNonPDFs" -> cmd.submissions.filter(
				_.allAttachments.exists(!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension))
			).map(submission =>
				Map(
					"submission" -> submission.id,
					"universityId" -> submission.universityId,
					"name" -> (if (module.adminDepartment.showStudentName) userLookup.getUserByUserId(submission.userId).getFullName else ""),
					"nonPDFFiles" -> submission.allAttachments.filter(
						!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension)
					).map(_.name)
				)
			)
		))).noLayout()
	}

	@RequestMapping(params = Array("download"))
	def download(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}

}
@Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/submissions.pdf"))
class DownloadAdminSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		DownloadAdminSubmissionsForPrintingCommand(module, assignment)

}

@Controller
@RequestMapping(value = Array("/coursework/admin/module/{module}/assignments/{assignment}/marker/{marker}/submissions.pdf"))
class DownloadMarkerSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable marker: User) =
		DownloadMarkerSubmissionsForPrintingCommand(module, assignment, marker, user)

}