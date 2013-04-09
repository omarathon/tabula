package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Department, MarkingWorkflow}
import uk.ac.warwick.tabula.data.model.MarkingMethod._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.permissions._

/** Abstract base command for either creating or editing a MarkingWorkflow */
abstract class ModifyMarkingWorkflowCommand(
	val department: Department) extends Command[MarkingWorkflow] with Daoisms with SelfValidating {

	var name: String = _
	var firstMarkers: JList[String] = JArrayList()
	var secondMarkers: JList[String] = JArrayList()
	var markingMethod: MarkingMethod = _

	// Subclasses can provide the "current" markingWorkflow if one applies, for validation.
	def currentMarkingWorkflow: Option[MarkingWorkflow]

	def contextSpecificValidation(errors: Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)

		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		if (department.markingWorkflows.exists(sameName)) {
			errors.rejectValue("name", "name.duplicate.markingWorkflow", Array(this.name), null)
		}

		if (markingMethod == null)
			errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.none")

		val firstMarkersValidator = new UsercodeListValidator(firstMarkers, "firstMarkers"){
			override def alreadyHasCode = hasDuplicates(firstMarkers)
		}
		firstMarkersValidator.validate(errors)

		// validate only when second markers are used
		if(markingMethod == SeenSecondMarking){
			val secondMarkersValidator = new UsercodeListValidator(secondMarkers, "secondMarkers"){
				override def alreadyHasCode = hasDuplicates(secondMarkers)
			}
			secondMarkersValidator.validate(errors)
		}

		// there is a marker in both lists
		val trimmedFirst = firstMarkers.map{ _.trim }.filterNot{ _.isEmpty }.toSet
		val trimmedSecond = secondMarkers.map{ _.trim }.filterNot{ _.isEmpty }.toSet
		if ((trimmedFirst & trimmedSecond).size > 0)
			errors.reject("markingWorkflow.markers.bothLists")
	}

	def hasDuplicates(markers:JList[_]):Boolean = {
		markers.distinct.size != markers.size
	}

	// If there's a current markingWorkflow, returns whether "other" is a different
	// scheme with the same name we're trying to use.
	// If there's no current markingWorkflow we just check if it's just the same name.
	def sameName(other: MarkingWorkflow) = currentMarkingWorkflow match {
		case Some(existing) =>
			other.id != existing.id && other.name == name
		case None =>
			other.name == name
	}

	// Called manually by controller.
	def doBind() {
	  firstMarkers = firstMarkers.filter(_.hasText)
		secondMarkers = secondMarkers.filter(_.hasText)
	}

	def copyTo(scheme: MarkingWorkflow) {
		scheme.name = name
		scheme.firstMarkers.includeUsers = firstMarkers
		scheme.secondMarkers.includeUsers = secondMarkers
		scheme.markingMethod = markingMethod
	}

	def copyFrom(scheme: MarkingWorkflow) {
		name = scheme.name
		firstMarkers.clear()
		firstMarkers.addAll(scheme.firstMarkers.includeUsers)
		secondMarkers.clear()
		secondMarkers.addAll(scheme.secondMarkers.includeUsers)
		markingMethod = scheme.markingMethod
	}

}