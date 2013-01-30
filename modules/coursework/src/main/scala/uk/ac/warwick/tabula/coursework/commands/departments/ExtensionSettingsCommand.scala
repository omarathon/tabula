package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.data.model.{UserGroup, Department}
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


class ExtensionSettingsCommand (val department:Department, val features:Features) extends Command[Unit] {
	
	PermissionCheck(Permission.Department.ManageExtensionSettings(), department)

	@BeanProperty var allowExtensionRequests:JBoolean =_
	@BeanProperty var extensionGuidelineSummary:String =_
	@BeanProperty var extensionGuidelineLink:String =_
	@BeanProperty var extensionManagers: JList[String] = ArrayList()

	val validUrl = """^((https?)://|(www2?)\.)[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$"""

	def validate(errors:Errors){
		if (features.extensions){
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
	}

	def copySettings() {
		if (features.extensions){
			allowExtensionRequests = department.allowExtensionRequests
			extensionGuidelineSummary = department.extensionGuidelineSummary
			extensionGuidelineLink = department.extensionGuidelineLink
			if (department.extensionManagers != null)
				extensionManagers.addAll(department.extensionManagers.includeUsers)
		}
	}

	override def applyInternal() {
		transactional() {
			if (features.extensions){
				department.allowExtensionRequests = allowExtensionRequests
				department.extensionGuidelineSummary = extensionGuidelineSummary
				department.extensionGuidelineLink = extensionGuidelineLink
				val managers = Option(department.extensionManagers).getOrElse {
					// existing departments will have a null value for managers. set it here
					department.extensionManagers = new UserGroup()
					department.extensionManagers
				}
				managers.setIncludeUsers(extensionManagers)
			}
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}