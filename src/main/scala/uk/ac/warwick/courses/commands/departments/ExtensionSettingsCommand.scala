package uk.ac.warwick.courses.commands.departments

import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.commands.{Description, Command}
import org.springframework.beans.factory.annotation.Configurable
import reflect.BeanProperty
import org.springframework.validation.Errors
import uk.ac.warwick.courses.helpers.StringUtils._
import uk.ac.warwick.courses.Features
import uk.ac.warwick.courses.data.Transactions._

class ExtensionSettingsCommand (val department:Department, val features:Features) extends Command[Unit] {

	@BeanProperty var allowExtensionRequests:JBoolean =_
	@BeanProperty var extensionGuidelineSummary:String =_
	@BeanProperty var extensionGuidelineLink:String =_

	val validUrl = """^((https?)://|(www2?)\.)[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$"""

	def validate(errors:Errors){
		if (features.extensions){
			if (allowExtensionRequests && !(extensionGuidelineSummary.hasText || extensionGuidelineLink.hasText)){
				errors.rejectValue("allowExtensionRequests", "department.settings.noExtensionGuidelines")
			}
			if(extensionGuidelineLink.hasText && !extensionGuidelineLink.matches(validUrl)){
				errors.rejectValue("extensionGuidelineLink", "department.settings.invalidURL")
			}
		}
	}

	def copySettings() {
		if (features.extensions){
			this.allowExtensionRequests = department.allowExtensionRequests
			this.extensionGuidelineSummary = department.extensionGuidelineSummary
			this.extensionGuidelineLink = department.extensionGuidelineLink
		}
	}

	override def apply() {
		transactional() {
			if (features.extensions){
				department.allowExtensionRequests = this.allowExtensionRequests
				department.extensionGuidelineSummary = this.extensionGuidelineSummary
				department.extensionGuidelineLink = this.extensionGuidelineLink
			}
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.properties("department" -> department.code)
	}
}