package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Department, MarkingWorkflow}
import uk.ac.warwick.tabula.data.model.MarkingMethod._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.services.MarkingWorkflowServiceComponent

/** Abstract base command for either creating or editing a MarkingWorkflow */
abstract class ModifyMarkingWorkflowCommand(val department: Department) 
	extends CommandInternal[MarkingWorkflow] with BindListener with MarkingWorkflowCommandState {
	self: MarkingWorkflowServiceComponent =>

	var name: String = _
	var firstMarkers: JList[String] = JArrayList()
	var secondMarkers: JList[String] = JArrayList()
	var markingMethod: MarkingMethod = _

	var firstMarkerRoleName: String = _
	var secondMarkerRoleName: String = _

	def onBind(result: BindingResult) {
	  firstMarkers = firstMarkers.asScala.filter(_.hasText).asJava
	  secondMarkers = secondMarkers.asScala.filter(_.hasText).asJava
	}

	def copyTo(scheme: MarkingWorkflow) {
		scheme.name = name
		scheme.firstMarkers.includeUsers = firstMarkers
		scheme.secondMarkers.includeUsers = secondMarkers
	}

	def copyFrom(scheme: MarkingWorkflow) {
		name = scheme.name
		firstMarkers.clear()
		firstMarkers.addAll(scheme.firstMarkers.includeUsers)
		secondMarkers.clear()
		secondMarkers.addAll(scheme.secondMarkers.includeUsers)
		markingMethod = scheme.markingMethod

		firstMarkerRoleName = scheme.firstMarkerRoleName
		secondMarkerRoleName = scheme.secondMarkerRoleName.getOrElse("Second marker")
	}

}

trait MarkingWorkflowCommandState {
	def department: Department
	def name: String
	def firstMarkers: JList[String]
	def secondMarkers: JList[String]
	def markingMethod: MarkingMethod

	def firstMarkerRoleName: String
	def secondMarkerRoleName: String
}

trait MarkingWorkflowCommandValidation extends SelfValidating {
	self: MarkingWorkflowCommandState =>
		
	// Subclasses can provide the "current" markingWorkflow if one applies, for validation.
	def currentMarkingWorkflow: Option[MarkingWorkflow]

	def contextSpecificValidation(errors: Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)

		rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		if (department.markingWorkflows.asScala.exists(sameName)) {
			errors.rejectValue("name", "name.duplicate.markingWorkflow", Array(this.name), null)
		}

		if (markingMethod == null)
			errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.none")

		val firstMarkersValidator = new UsercodeListValidator(firstMarkers, "firstMarkers"){
			override def alreadyHasCode = hasDuplicates(firstMarkers.asScala)
		}
		firstMarkersValidator.validate(errors)

		// validate only when second markers are used
		if(markingMethod == SeenSecondMarking){
			val secondMarkersValidator = new UsercodeListValidator(secondMarkers, "secondMarkers"){
				override def alreadyHasCode = hasDuplicates(secondMarkers.asScala)
			}
			secondMarkersValidator.validate(errors)
		}

		// there is a marker in both lists
		val trimmedFirst = firstMarkers.asScala.map{ _.trim }.filterNot{ _.isEmpty }.toSet
		val trimmedSecond = secondMarkers.asScala.map{ _.trim }.filterNot{ _.isEmpty }.toSet
		if ((trimmedFirst & trimmedSecond).size > 0)
			errors.reject("markingWorkflow.markers.bothLists")
	}

	def hasDuplicates(strings: Seq[String]): Boolean = {
		strings.filter { _.hasText }.distinct.size != strings.filter { _.hasText }.size
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
}