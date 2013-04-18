package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkingWorkflowDao
import uk.ac.warwick.tabula.permissions._

/** Edit an existing markingWorkflow. */
class EditMarkingWorkflowCommand(department: Department, val markingWorkflow: MarkingWorkflow) extends ModifyMarkingWorkflowCommand(department) {

	mustBeLinked(markingWorkflow, department)
	PermissionCheck(Permissions.MarkingWorkflow.Update, markingWorkflow)

	var dao = Wire[MarkingWorkflowDao]

	def hasExistingSubmissions: Boolean = dao.getAssignmentsUsingMarkingWorkflow(markingWorkflow).exists(!_.submissions.isEmpty)

	// fill in the properties on construction
	copyFrom(markingWorkflow)

	def contextSpecificValidation(errors:Errors){
		if (hasExistingSubmissions){

			if (markingWorkflow.markingMethod != markingMethod)
				errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.submissionsExist")

			if (markingWorkflow.studentsChooseMarker){
				val existingFirstMarkers = markingWorkflow.firstMarkers.includeUsers.toSet
				val newFirstMarkers = firstMarkers.toSet
				val existingSecondMarkers = markingWorkflow.secondMarkers.includeUsers.toSet
				val newSecondMarkers = secondMarkers.toSet
				// if newMarkers is not a super set of existingMarker, markers have been removed.
				if (!(existingFirstMarkers -- newFirstMarkers).isEmpty || !(existingSecondMarkers -- newSecondMarkers).isEmpty){
					errors.rejectValue("firstMarkers", "markingWorkflow.firstMarkers.cannotRemoveMarkers")
				}
			}
		}
	}

	def applyInternal() = {
		transactional() {
			this.copyTo(markingWorkflow)
			session.update(markingWorkflow)
			markingWorkflow
		}
	}

	def currentMarkingWorkflow = Some(markingWorkflow)

	override def validate(errors: Errors) {
		super.validate(errors)
	}

	def describe(d: Description) = d.department(department).markingWorkflow(markingWorkflow)
}