package uk.ac.warwick.tabula.commands.cm2.departments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.departments.ExtensionSettingsCommand.Result
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object ExtensionSettingsCommand {
	type Result = Department
	type Command = Appliable[Result] with ExtensionSettingsCommandState with SelfValidating

	def apply(department: Department): Command =
		new ExtensionSettingsCommandInternal(department)
			with ComposableCommand[Result]
			with ExtensionSettingsCommandPermissions
			with ExtensionSettingsCommandValidation
			with ExtensionSettingsCommandDescription
			with AutowiringFeaturesComponent
			with AutowiringModuleAndDepartmentServiceComponent
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
	self: ModuleAndDepartmentServiceComponent with FeaturesComponent =>

	override def applyInternal(): Result = transactional() {
		if (features.extensions) {
			department.allowExtensionRequests = allowExtensionRequests
			department.extensionGuidelineSummary = extensionGuidelineSummary
			department.extensionGuidelineLink = extensionGuidelineLink
			department.extensionManagers.knownType.includedUserIds = extensionManagers.asScala

			moduleAndDepartmentService.saveOrUpdate(department)
		}

		department
	}
}

trait ExtensionSettingsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ExtensionSettingsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		p.PermissionCheck(Permissions.Department.ManageExtensionSettings, mandatory(department))

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

	override def describe(d: Description): Unit = d.department(department)
}