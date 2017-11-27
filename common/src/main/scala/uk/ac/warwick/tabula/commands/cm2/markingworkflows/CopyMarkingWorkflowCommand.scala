package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Describable, Description, _}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}


object CopyMarkingWorkflowCommand {

	type Command = Appliable[CM2MarkingWorkflow] with CopyMarkingWorkflowState

	def apply(department:Department, markingWorkflow: CM2MarkingWorkflow) =
		new CopyMarkingWorkflowCommandInternal(department, markingWorkflow)
			with ComposableCommand[CM2MarkingWorkflow]
			with MarkingWorkflowPermissions
			with CopyMarkingWorkflowDescription
			with CopyMarkingWorkflowState
			with CopyMarkingWorkflowValidation
			with AutowiringCM2MarkingWorkflowServiceComponent
}

class CopyMarkingWorkflowCommandInternal(val department: Department, val markingWorkflow: CM2MarkingWorkflow)
	extends CommandInternal[CM2MarkingWorkflow] {

	self: CopyMarkingWorkflowState with CM2MarkingWorkflowServiceComponent =>

	def applyInternal(): CM2MarkingWorkflow = {
		val (markersAUsers, markersBUsers) = markingWorkflow.markersByRole.values.toList match {
			case a :: rest => (a, rest.headOption.getOrElse(Nil))
			case _ => throw new IllegalArgumentException(s"workflow ${markingWorkflow.id} has no markers")
		}

		val unproxiedWorkflow = HibernateHelpers.initialiseAndUnproxy(markingWorkflow)
		val newWorkflow = unproxiedWorkflow match {
			case w: DoubleWorkflow => DoubleWorkflow(w.name, department, markersAUsers, markersBUsers)
			case w: ModeratedWorkflow => ModeratedWorkflow(w.name, department, w.moderationSampler, markersAUsers, markersBUsers)
			case w: SingleMarkerWorkflow => SingleMarkerWorkflow(w.name, department, markersAUsers)
			case w: DoubleBlindWorkflow => DoubleBlindWorkflow(w.name, department, markersAUsers, markersBUsers)
			case _ => throw new UnsupportedOperationException(markingWorkflow.workflowType + " not specified")
		}
		newWorkflow.academicYear = currentAcademicYear
		newWorkflow.name = markingWorkflow.name
		newWorkflow.isReusable = markingWorkflow.isReusable
		cm2MarkingWorkflowService.save(newWorkflow)
		newWorkflow
	}
}

trait CopyMarkingWorkflowValidation extends SelfValidating {

	self: CopyMarkingWorkflowState =>

	override def validate(errors: Errors): Unit = {
		if (department.cm2MarkingWorkflows.exists(w =>
			w.academicYear == currentAcademicYear && w.name == markingWorkflow.name)
		){
			errors.rejectValue("markingWorkflow", "name.duplicate.markingWorkflow", Array(markingWorkflow.name), null)
		}
	}

}

trait CopyMarkingWorkflowDescription extends Describable[CM2MarkingWorkflow] {
	self: CopyMarkingWorkflowState =>

	override lazy val eventName: String = "CopyMarkingWorkflow"

	def describe(d: Description) {
		d.department(department)
		d.markingWorkflow(markingWorkflow)
	}
}

trait CopyMarkingWorkflowState {
	def department: Department
	def markingWorkflow: CM2MarkingWorkflow
	val currentAcademicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
}

trait CopyMarkingWorkflowComponent {
	def copyMarkingWorkflow(department: Department, workflow: CM2MarkingWorkflow): CM2MarkingWorkflow
}

trait CopyMarkingWorkflowCommandComponent extends CopyMarkingWorkflowComponent {
	def copyMarkingWorkflow(department: Department, workflow: CM2MarkingWorkflow): CM2MarkingWorkflow =
		CopyMarkingWorkflowCommand(department, workflow).apply()
}