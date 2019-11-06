package uk.ac.warwick.tabula.commands.cm2.departments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.departments.ExtensionSettingsCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.jdk.CollectionConverters._
import scala.reflect.classTag

object ExtensionSettingsCommand {
  type Result = Department
  type Command = Appliable[Result] with ExtensionSettingsCommandState with SelfValidating

  val AdminPermission: Permission = Permissions.Department.ManageExtensionSettings

  def apply(department: Department): Command =
    new ExtensionSettingsCommandInternal(department)
      with ComposableCommand[Result]
      with ExtensionSettingsCommandPermissions
      with ExtensionSettingsCommandValidation
      with ExtensionSettingsCommandDescription
      with AutowiringFeaturesComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringPermissionsServiceComponent
}

trait ExtensionSettingsCommandState {
  def department: Department
}

trait ExtensionSettingsCommandRequest {
  self: ExtensionSettingsCommandState =>

  var allowExtensionRequests: Boolean = department.allowExtensionRequests
  var extensionGuidelineSummary: String = department.extensionGuidelineSummary
  var extensionGuidelineLink: String = department.extensionGuidelineLink
  var extensionManagers: JList[String] = JArrayList()
  extensionManagers.addAll(department.extensionManagers.knownType.includedUserIds.asJava)
}

class ExtensionSettingsCommandInternal(val department: Department)
  extends CommandInternal[Result] with ExtensionSettingsCommandState with ExtensionSettingsCommandRequest {
  self: ModuleAndDepartmentServiceComponent
    with FeaturesComponent
    with PermissionsServiceComponent =>

  override def applyInternal(): Result = transactional() {
    if (features.extensions) {
      department.allowExtensionRequests = allowExtensionRequests
      department.extensionGuidelineSummary = extensionGuidelineSummary
      department.extensionGuidelineLink = extensionGuidelineLink

      // Collect any users that we're changing the permissions of
      val oldUserIds = department.extensionManagers.knownType.includedUserIds
      val newUserIds = extensionManagers.asScala.toSet

      val removedUsers = oldUserIds.diff(newUserIds)
      val addedUsers = newUserIds.diff(oldUserIds)

      department.extensionManagers.knownType.includedUserIds = extensionManagers.asScala.toSet

      // Clear permissions cache otherwise added extension managers still won't be able to see the screen
      (removedUsers ++ addedUsers).foreach { usercode =>
        permissionsService.clearCachesForUser((usercode, classTag[Department]))
      }

      moduleAndDepartmentService.saveOrUpdate(department)
    }

    department
  }
}

trait ExtensionSettingsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ExtensionSettingsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(department))

}

trait ExtensionSettingsCommandValidation extends SelfValidating {
  self: ExtensionSettingsCommandRequest with ExtensionSettingsCommandState =>

  val validUrl = """^((https?)://|(www2?)\.)[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$"""

  override def validate(errors: Errors): Unit = {
    if (allowExtensionRequests) {
      if (!(extensionGuidelineSummary.hasText || extensionGuidelineLink.hasText)) {
        errors.rejectValue("allowExtensionRequests", "department.settings.noExtensionGuidelines")
      }

      val firstMarkersValidator = new UsercodeListValidator(extensionManagers, "extensionManagers")
      firstMarkersValidator.validate(errors)
    }

    if (extensionGuidelineLink.hasText && !extensionGuidelineLink.matches(validUrl)) {
      errors.rejectValue("extensionGuidelineLink", "department.settings.invalidURL")
    }
  }
}

trait ExtensionSettingsCommandDescription extends Describable[Result] {
  self: ExtensionSettingsCommandState =>

  override lazy val eventName: String = "ExtensionSettings"

  override def describe(d: Description): Unit = d.department(department)
}
