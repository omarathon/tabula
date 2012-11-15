package uk.ac.warwick.courses.commands.markschemes

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.springframework.validation.Errors
import javax.persistence.Entity
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.commands.SelfValidating
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.Transactions._
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.data.model.MarkScheme
import uk.ac.warwick.courses.helpers.ArrayList
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.validators.UsercodeListValidator

/** Abstract base command for either creating or editing a MarkScheme */
abstract class ModifyMarkSchemeCommand(
	@BeanProperty val department: Department) extends Command[MarkScheme] with Daoisms with SelfValidating {

	@BeanProperty var name: String = _
	@BeanProperty var firstMarkers: JList[String] = ArrayList()
	@BeanProperty var studentsChooseMarker: Boolean = _
	
	// Subclasses can provide the "current" markscheme if one applies, for validation.
	def currentMarkScheme: Option[MarkScheme]

	def validate(implicit errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")
		
		if (department.markSchemes.exists(sameName)) {
			errors.rejectValue("name", "name.duplicate.markScheme", Array(this.name), null)
		}
		
		val firstMarkersValidator = new UsercodeListValidator(firstMarkers, "firstMarkers")
		firstMarkersValidator.validate(errors)
		
	}
	
	// If there's a current markscheme, returns whether "other" is a different
	// scheme with the same name we're trying to use.
	// If there's no current markscheme we just check if it's just the same name.
	def sameName(other: MarkScheme) = currentMarkScheme match {
		case Some(existing) => 
			other.id != existing.id && other.name == name
		case None => 
			other.name == name
	}
		
	// Called manually by controller.
	def doBind() {
	  firstMarkers = firstMarkers.filter(StringUtils.hasText)
	}

	def copyTo(scheme: MarkScheme) {
		scheme.name = name
		scheme.firstMarkers.setIncludeUsers(firstMarkers)
		scheme.studentsChooseMarker = studentsChooseMarker
	}

	def copyFrom(scheme: MarkScheme) {
		name = scheme.name
		firstMarkers.clear()
		firstMarkers.addAll(scheme.firstMarkers.includeUsers)
		studentsChooseMarker = scheme.studentsChooseMarker
	}

}