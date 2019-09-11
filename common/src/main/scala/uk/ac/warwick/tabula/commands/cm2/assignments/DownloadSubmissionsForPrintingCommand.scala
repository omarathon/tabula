package uk.ac.warwick.tabula.commands.cm2.assignments

import java.io.ByteArrayOutputStream

import com.google.common.io.ByteSource
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.DownloadSubmissionsForPrintingCommand.Result
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, MarkerFeedback, Submission}
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.pdf.{CombinesPdfs, FreemarkerXHTMLPDFGeneratorComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{AutowiringTopLevelUrlComponent, CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._

trait DownloadAdminSubmissionsForPrintingCommandHelper
  extends FreemarkerXHTMLPDFGeneratorComponent
    with AutowiredTextRendererComponent
    with PhotosWarwickMemberPhotoUrlGeneratorComponent
    with AutowiringFileDaoComponent
    with AutowiringTopLevelUrlComponent
    with CombinesPdfs

object DownloadSubmissionsForPrintingCommand {
  type Result = RenderableFile
  type Command = Appliable[Result] with DownloadSubmissionsForPrintingCommandRequest
}

object DownloadAdminSubmissionsForPrintingCommand {
  type Command = DownloadSubmissionsForPrintingCommand.Command with DownloadSubmissionsForPrintingCommandState

  final val receiptTemplate = "/WEB-INF/freemarker/cm2/submit/submission-receipt.ftlh"
  final val nonPDFTemplate = "/WEB-INF/freemarker/coursework/admin/assignments/submissionsandfeedback/non-pdf-attachments.ftl"
  final val pdfExtension = ".pdf"

  def apply(assignment: Assignment): Command =
    new DownloadSubmissionsForPrintingCommandInternal(assignment, null, null)
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with DownloadAdminSubmissionsForPrintingCommandHelper
      with DownloadAdminSubmissionsForPrintingPermissions
      with DownloadSubmissionsForPrintingCommandState
      with DownloadAdminSubmissionsForPrintingCommandRequest
}

object DownloadMarkerSubmissionsForPrintingCommand {
  type Command = DownloadSubmissionsForPrintingCommand.Command with DownloadMarkerSubmissionsForPrintingCommandState

  def apply(assignment: Assignment, marker: User, submitter: CurrentUser): Command =
    new DownloadSubmissionsForPrintingCommandInternal(assignment, marker, submitter)
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with DownloadAdminSubmissionsForPrintingCommandHelper
      with DownloadMarkerSubmissionsForPrintingPermissions
      with DownloadMarkerSubmissionsForPrintingCommandState
      with DownloadMarkerSubmissionsForPrintingCommandRequest
}


class DownloadSubmissionsForPrintingCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser)
  extends CommandInternal[Result] {

  self: DownloadSubmissionsForPrintingCommandRequest with FreemarkerXHTMLPDFGeneratorComponent
    with FileDaoComponent with CombinesPdfs =>

  override def applyInternal(): RenderableAttachment = {
    if (submissions.isEmpty) throw new ItemNotFoundException

    val parts: Seq[FileAttachment] = submissions.flatMap(submission => {
      Seq(
        doReceipt(submission),
        doNonPDFs(submission)
      ).flatten ++ getPDFs(submission)
    })

    new RenderableAttachment(combinePdfs(parts, "submissions.pdf"))
  }

  private def doReceipt(submission: Submission): Option[FileAttachment] = {
    val output = new ByteArrayOutputStream()
    pdfGenerator.renderTemplate(
      DownloadAdminSubmissionsForPrintingCommand.receiptTemplate,
      Map(
        "submission" -> submission
      ),
      output
    )
    val pdf = new FileAttachment
    pdf.name = s"submission-receipt-${submission.studentIdentifier}.pdf"
    pdf.uploadedData = ByteSource.wrap(output.toByteArray)
    fileDao.saveTemporary(pdf)
    Some(pdf)
  }

  private def doNonPDFs(submission: Submission): Option[FileAttachment] = {
    submission.allAttachments.filter(!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension)).map(_.name) match {
      case Nil => None
      case attachments =>
        val output = new ByteArrayOutputStream()
        pdfGenerator.renderTemplate(
          DownloadAdminSubmissionsForPrintingCommand.nonPDFTemplate,
          Map(
            "attachments" -> attachments
          ),
          output
        )
        val pdf = new FileAttachment
        pdf.name = s"non-pdf-attachments-${submission.studentIdentifier}.pdf"
        pdf.uploadedData = ByteSource.wrap(output.toByteArray)
        fileDao.saveTemporary(pdf)
        Some(pdf)
    }
  }

  private def getPDFs(submission: Submission): Seq[FileAttachment] =
    submission.allAttachments.filter(_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension))

}

trait DownloadAdminSubmissionsForPrintingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: DownloadSubmissionsForPrintingCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.Submission.Read, assignment)
  }

}

trait DownloadMarkerSubmissionsForPrintingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: DownloadMarkerSubmissionsForPrintingCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.Submission.Read, assignment)
    if (submitter.apparentUser != marker) {
      p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
    }
  }

}

trait DownloadSubmissionsForPrintingCommandState extends SelectedStudentsState {
  def assignment: Assignment
}

trait DownloadMarkerSubmissionsForPrintingCommandState extends DownloadSubmissionsForPrintingCommandState {
  def marker: User

  def submitter: CurrentUser
}

trait DownloadSubmissionsForPrintingCommandRequest extends SelectedStudentsRequest {
  self: DownloadSubmissionsForPrintingCommandState =>
}

trait DownloadAdminSubmissionsForPrintingCommandRequest extends DownloadSubmissionsForPrintingCommandRequest {
  self: DownloadSubmissionsForPrintingCommandState =>
}

trait DownloadMarkerSubmissionsForPrintingCommandRequest extends DownloadSubmissionsForPrintingCommandRequest {
  self: DownloadMarkerSubmissionsForPrintingCommandState =>

  var markerFeedback: JList[MarkerFeedback] = JArrayList()

  override def submissions: Seq[Submission] = {
    val selectedStudents =
      if (markerFeedback.isEmpty) students.asScala
      else markerFeedback.asScala.map(_.student.getUserId)

    val allMarkerSubmissions =
      if (assignment.cm2Assignment)
        assignment.cm2MarkerSubmissions(marker)
      else
        assignment.getMarkersSubmissions(marker).filter { submission =>
          val markerFeedback = assignment.getMarkerFeedbackForCurrentPosition(submission.usercode, marker)
          markerFeedback.exists(mf => mf.state != MarkingCompleted)
        }

    if (selectedStudents.isEmpty) allMarkerSubmissions
    else selectedStudents.flatMap { s => allMarkerSubmissions.find(_.usercode == s) }
  }
}
