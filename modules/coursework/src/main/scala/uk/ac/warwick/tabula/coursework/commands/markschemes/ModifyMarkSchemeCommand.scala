package uk.ac.warwick.tabula.coursework.commands.markschemes

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.MarkScheme
import uk.ac.warwick.tabula.helpers.ArrayList
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.tabula.validators.UsercodeListValidator

/** Abstract base command for either creating or editing a MarkScheme */
abstract class ModifyMarkSchemeCommand(
	@BeanProperty val department: Department) extends Command[MarkScheme] with Daoisms with SelfValidating {

	@BeanProperty var name: String = _
	@BeanProperty var firstMarkers: JList[String] = ArrayList()
	@BeanProperty var secondMarkers: JList[String] = ArrayList()
	@BeanProperty var studentsChooseMarker: Boolean = _
	
	// Subclasses can provide the "current" markscheme if one applies, for validation.
	def currentMarkScheme: Option[MarkScheme]

	def contextSpecificValidation(errors: Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)

		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")
		
		if (department.markSchemes.exists(sameName)) {
			errors.rejectValue("name", "name.duplicate.markScheme", Array(this.name), null)
		}
		
		val firstMarkersValidator = new UsercodeListValidator(firstMarkers, "firstMarkers")
		firstMarkersValidator.validate(errors)
		val secondMarkersValidator = new UsercodeListValidator(secondMarkers, "secondMarkers")
		secondMarkersValidator.validate(errors)
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
		secondMarkers = secondMarkers.filter(StringUtils.hasText)
	}

	def copyTo(scheme: MarkScheme) {
		scheme.name = name
		scheme.firstMarkers.setIncludeUsers(firstMarkers)
		scheme.secondMarkers.setIncludeUsers(secondMarkers)
		scheme.studentsChooseMarker = studentsChooseMarker
	}

	def copyFrom(scheme: MarkScheme) {
		name = scheme.name
		firstMarkers.clear()
		firstMarkers.addAll(scheme.firstMarkers.includeUsers)
		secondMarkers.clear()
		secondMarkers.addAll(scheme.secondMarkers.includeUsers)
		studentsChooseMarker = scheme.studentsChooseMarker
	}

}