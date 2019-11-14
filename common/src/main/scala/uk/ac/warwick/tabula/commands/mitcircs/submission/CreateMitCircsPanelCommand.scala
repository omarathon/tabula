package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.{LocalDate, LocalTime}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.CreateMitCircsPanelCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.{Department, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.reflect.classTag

object CreateMitCircsPanelCommand {

  type Result = MitigatingCircumstancesPanel
  type Command = Appliable[Result] with CreateMitCircsPanelState with CreateMitCircsPanelRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesPanel.Modify

  def apply(department: Department, year: AcademicYear, currentUser: User) = new CreateMitCircsPanelCommandInternal(department, year, currentUser)
    with ComposableCommand[MitigatingCircumstancesPanel]
    with CreateMitCircsPanelRequest
    with ModifyMitCircsPanelValidation
    with CreateMitCircsPanelPermissions
    with CreateMitCircsPanelDescription
    with AutowiringMitCircsPanelServiceComponent
    with AutowiringUserLookupComponent
    with AutowiringPermissionsServiceComponent
}

abstract class CreateMitCircsPanelCommandInternal(val department: Department, val year: AcademicYear, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesPanel] with CreateMitCircsPanelState {
  self: CreateMitCircsPanelRequest with MitCircsPanelServiceComponent with UserLookupComponent with PermissionsServiceComponent =>

  def applyInternal(): MitigatingCircumstancesPanel = transactional() {
    val transientPanel = new MitigatingCircumstancesPanel(department, year)
    transientPanel.name = name
    if(date != null) {
      if(start != null) transientPanel.date = date.toDateTime(start)
      if(end != null) transientPanel.endDate = date.toDateTime(end)
    }
    if (locationId.hasText) {
      transientPanel.location = MapLocation(location, locationId)
    } else if (location.hasText) {
      transientPanel.location = NamedLocation(location)
    }
    submissions.asScala.foreach(transientPanel.addSubmission)
    if(chair.hasText) transientPanel.chair = userLookup.getUserByUserId(chair)
    if(secretary.hasText) transientPanel.secretary = userLookup.getUserByUserId(secretary)
    val panel = mitCircsPanelService.saveOrUpdate(transientPanel)
    val viewers = (members.asScala.toSet ++ Set(chair, secretary)).filter(_.hasText)
    panel.viewers = viewers
    viewers.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[MitigatingCircumstancesPanel]))
    }

    mitCircsPanelService.saveOrUpdate(transientPanel)
  }
}

trait CreateMitCircsPanelPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateMitCircsPanelState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, department)
  }
}

trait ModifyMitCircsPanelValidation extends SelfValidating {
  self: ModifyMitCircsPanelRequest
    with UserLookupComponent =>

  def validate(errors: Errors): Unit = {
    if(!name.hasText) errors.rejectValue("name", "mitigatingCircumstances.panel.name.required")

    if (chair.hasText) {
      val usercodeValidator = new UsercodeListValidator(JArrayList(chair), "chair", staffOnlyForADS = true)
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }

    if (secretary.hasText) {
      val usercodeValidator = new UsercodeListValidator(JArrayList(secretary), "secretary", staffOnlyForADS = true)
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }

    if (!members.isEmpty) {
      val usercodeValidator = new UsercodeListValidator(members, "members", staffOnlyForADS = true)
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }
  }
}

trait CreateMitCircsPanelDescription extends Describable[MitigatingCircumstancesPanel] {
  self: CreateMitCircsPanelState =>

  override lazy val eventName: String =  "CreateMitCircsPanel"

  def describe(d: Description): Unit =
    d.department(department)

  override def describeResult(d: Description, result: Result): Unit =
    d.properties("mitCircsSubmissions" -> result.submissions.map(_.id))
}

trait CreateMitCircsPanelState {
  val department: Department
  val year: AcademicYear
  val currentUser: User
}

trait CreateMitCircsPanelRequest extends ModifyMitCircsPanelRequest {
  self: CreateMitCircsPanelState =>

  chair = currentUser.getUserId
}

trait ModifyMitCircsPanelRequest {
  var name: String = _
  var date: LocalDate = _
  var start: LocalTime = _
  var end: LocalTime =_
  var location: String = _
  var locationId: String = _
  var submissions: JList[MitigatingCircumstancesSubmission] = JArrayList()
  var chair: String = _
  var secretary: String = _
  var members: JList[String] = JArrayList()
}
