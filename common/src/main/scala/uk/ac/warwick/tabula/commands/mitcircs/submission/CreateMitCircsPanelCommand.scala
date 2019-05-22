package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import CreateMitCircsPanelCommand._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}

import scala.collection.JavaConverters._

object CreateMitCircsPanelCommand {

  type Result = MitigatingCircumstancesPanel
  type Command = Appliable[Result] with CreateMitCircsPanelState with CreateMitCircsPanelRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesPanel.Modify

  def apply(department: Department, year: AcademicYear) = new CreateMitCircsPanelCommandInternal(department, year)
    with ComposableCommand[MitigatingCircumstancesPanel]
    with CreateMitCircsPanelRequest
    with CreateMitCircsPanelValidation
    with CreateMitCircsPanelPermissions
    with CreateMitCircsPanelDescription
    with AutowiringMitCircsPanelServiceComponent
}

class CreateMitCircsPanelCommandInternal(val department: Department, val year: AcademicYear)
  extends CommandInternal[MitigatingCircumstancesPanel] with CreateMitCircsPanelState with CreateMitCircsPanelValidation {

  self: CreateMitCircsPanelRequest with MitCircsPanelServiceComponent =>

  def applyInternal(): MitigatingCircumstancesPanel = transactional() {
    val panel = new MitigatingCircumstancesPanel(department, year)
    panel.name = name
    panel.date = date
    submissions.asScala.foreach(panel.addSubmission)
    members.asScala.foreach(panel.members.add)
    mitCircsPanelService.saveOrUpdate(panel)
    panel
  }
}

trait CreateMitCircsPanelPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateMitCircsPanelState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, department)
  }
}

trait CreateMitCircsPanelValidation extends SelfValidating {
  self: CreateMitCircsPanelRequest =>
  def validate(errors: Errors) {
    if(!name.hasText) errors.rejectValue("name", "mitigatingCircumstances.panel.name.required")
  }
}

trait CreateMitCircsPanelDescription extends Describable[MitigatingCircumstancesPanel] {
  self: CreateMitCircsPanelState =>

  override lazy val eventName: String =  "CreateMitCircsPanel"

  def describe(d: Description) {
    d.properties("department" -> department)
  }
}

trait CreateMitCircsPanelState {
  val department: Department
  val year: AcademicYear
}

trait CreateMitCircsPanelRequest {
  var name: String = _
  var date: LocalDate = _
  var submissions: JList[MitigatingCircumstancesSubmission] = JArrayList()
  var members: JList[User] = JArrayList()
}