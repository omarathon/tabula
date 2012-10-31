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
import uk.ac.warwick.courses.helpers.StringUtils._
import uk.ac.warwick.courses.commands.Command

/** Abstract base command for either creating or editing a MarkScheme */
abstract class ModifyMarkSchemeCommand(
	@BeanProperty val department: Department) extends Command[MarkScheme] with Daoisms with SelfValidating {

	@BeanProperty var name: String = _
	@BeanProperty var firstMarkers: JList[String] = ArrayList()
	@BeanProperty var studentsChooseMarker: Boolean = _

	def validate(implicit errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")
	}

	// Called manually by controller.
	def doBind() {
		def removeEmpty(strings: JList[String]) = strings.filter{_.hasText}
	  firstMarkers = removeEmpty(firstMarkers)
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