package uk.ac.warwick.tabula.commands.sysadmin

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CheckTCAInProgressCommand {
  def apply() =
    new CheckTCAInProgressCommandInternal
    with ComposableCommand[Seq[OriginalityReport]]
    with CheckTCAInProgressCommandPermissions
    with Unaudited
    with AutowiringOriginalityReportServiceComponent
  with CheckTCAInProgressRequest
}

class CheckTCAInProgressCommandInternal extends CommandInternal[Seq[OriginalityReport]] {
  self: OriginalityReportServiceComponent
  with CheckTCAInProgressRequest =>

  override def applyInternal(): Seq[OriginalityReport] = {
    originalityReportService.getIncompleteTcaSubmissions(since)
  }
}

trait CheckTCAInProgressCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.CheckForPlagiarism, PermissionsTarget.Global)
  }
}

trait CheckTCAInProgressRequest {
  var since: LocalDate = LocalDate.now.minusDays(1)
}
