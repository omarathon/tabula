package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.data.model.{UserGroup, Department}
import uk.ac.warwick.tabula.data.model.Department.Settings
import uk.ac.warwick.tabula.commands.{Description, Command}
import reflect.BeanProperty
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.SelfValidating


class ExtensionSettingsCommand (val department:Department, val features:Features) extends Command[Unit] with SelfValidating {
	
	PermissionCheck(Permissions.Department.ManageExtensionSettings, department)

	var allowExtensionRequests:Boolean = department.allowExtensionRequests
	var extensionGuidelineSummary:String = department.extensionGuidelineSummary
	var extensionGuidelineLink:String = department.extensionGuidelineLink
	var extensionManagers: JList[String] = ArrayList()
	
	extensionManagers.addAll(department.extensionManagers.includeUsers)

	val validUrl = """^((https?)://|(www2?)\.)[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$"""

	override def validate(errors:Errors) {
		if (allowExtensionRequests){
			if(!(extensionGuidelineSummary.hasText || extensionGuidelineLink.hasText)){
				errors.rejectValue("allowExtensionRequests", "department.settings.noExtensionGuidelines")
			}
			val firstMarkersValidator = new UsercodeListValidator(extensionManagers, "extensionManagers")
			firstMarkersValidator.validate(errors)
		}
		if(extensionGuidelineLink.hasText && !extensionGuidelineLink.matches(validUrl)){
			errors.rejectValue("extensionGuidelineLink", "department.settings.invalidURL")
		}
	}

	override def applyInternal() {
		transactional() {
			if (features.extensions){
				department.allowExtensionRequests = allowExtensionRequests
				department.extensionGuidelineSummary = extensionGuidelineSummary
				department.extensionGuidelineLink = extensionGuidelineLink

				department.extensionManagers.includeUsers = extensionManagers
			}
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}