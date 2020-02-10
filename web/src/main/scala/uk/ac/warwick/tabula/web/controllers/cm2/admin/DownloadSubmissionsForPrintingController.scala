package uk.ac.warwick.tabula.web.controllers.cm2.admin

import com.itextpdf.text.pdf.PdfReader
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.{DownloadAdminSubmissionsForPrintingCommand, DownloadMarkerSubmissionsForPrintingCommand, DownloadSubmissionsForPrintingCommand}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.fileserver.ContentDisposition
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

import scala.util.Try

trait DownloadSubmissionsForPrintingController extends CourseworkController with AutowiringUserLookupComponent {

  private def isValidPdf(file: FileAttachment): Boolean = file.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension) &&
    Try(new PdfReader(file.asByteSource.openStream()).close()).isSuccess

  @RequestMapping
  def pdfCheck(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand.Command, @PathVariable assignment: Assignment): Mav = {
    Mav(new JSONView(Map(
      "submissionsWithNonPDFs" -> cmd.submissions
        .map(s => (s, s.allAttachments.filter(file => !isValidPdf(file))))
        .filter { case (_, a) => a.nonEmpty }
        .map { case (s, a) =>
          Map(
            "submission" -> s.id,
            "universityId" -> s.studentIdentifier,
            "name" -> (if (assignment.module.adminDepartment.showStudentName) userLookup.getUserByUserId(s.usercode).getFullName else ""),
            "nonPDFFiles" -> a.map(_.name)
          )
        }
    ))).noLayout()
  }

  @RequestMapping(params = Array("download"))
  def download(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand.Command): Mav = {
    Mav(new RenderableFileView(cmd.apply().withContentDisposition(ContentDisposition.Attachment)))
  }

}

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/submissions.pdf"))
class DownloadAdminSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment): DownloadSubmissionsForPrintingCommand.Command =
    DownloadAdminSubmissionsForPrintingCommand(assignment)

}

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/marker/{marker}/submissions.pdf"))
class DownloadMarkerSubmissionsForPrintingController extends DownloadSubmissionsForPrintingController {

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment, @PathVariable marker: User, submitter: CurrentUser): DownloadSubmissionsForPrintingCommand.Command =
    DownloadMarkerSubmissionsForPrintingCommand(assignment, marker, submitter)

}
