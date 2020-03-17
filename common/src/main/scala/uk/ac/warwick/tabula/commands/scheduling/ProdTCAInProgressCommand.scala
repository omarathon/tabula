package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin.{TurnitinTcaRetrySimilarityReportCommand, TurnitinTcaRetrySubmissionCommand}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.turnitintca.TcaSubmissionStatus
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ProdTCAInProgressCommand {
  def apply() =
    new ProdTCAInProgressCommandInternal()
    with ComposableCommand[Seq[OriginalityReport]]
    with ProdTCAInProgressCommandPermissions
    with ProdTCAInProgressCommandDescription
    with AutowiringOriginalityReportServiceComponent
    with ProdTCAInProgressRequest
}

class ProdTCAInProgressCommandInternal() extends CommandInternal[Seq[OriginalityReport]] {
  self: OriginalityReportServiceComponent with ProdTCAInProgressRequest =>

  override def applyInternal(): Seq[OriginalityReport] = {

    val incompleteTCASubmissions = originalityReportService.getIncompleteTcaSubmissions(since)

    incompleteTCASubmissions.foreach {
      case originalityReport: OriginalityReport
        if originalityReport.tcaSubmissionStatus == TcaSubmissionStatus.Created ||
          originalityReport.tcaSubmissionStatus == TcaSubmissionStatus.Processing =>
        TurnitinTcaRetrySubmissionCommand(originalityReport.attachment.submissionValue.submission.assignment, originalityReport.attachment).apply()

      case originalityReport: OriginalityReport
        if originalityReport.tcaSubmissionStatus == TcaSubmissionStatus.Complete =>
        TurnitinTcaRetrySimilarityReportCommand(originalityReport.attachment.submissionValue.submission.assignment, originalityReport.attachment).apply()

    }
    incompleteTCASubmissions
  }
}

trait ProdTCAInProgressCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.CheckForPlagiarism, PermissionsTarget.Global)
  }
}

trait ProdTCAInProgressCommandDescription extends Describable[Seq[OriginalityReport]] {
  self: ProdTCAInProgressRequest =>

  def describe(d: Description): Unit = d.property("since" -> since)

  override def describeResult(d: Description, result: Seq[OriginalityReport]): Unit = {
    d.property("since" -> since)
    d.property("total" -> result.size)
  }
}

trait ProdTCAInProgressRequest {
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var since: LocalDate = _
}
