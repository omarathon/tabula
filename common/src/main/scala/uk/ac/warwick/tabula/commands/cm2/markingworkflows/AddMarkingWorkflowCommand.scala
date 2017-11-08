package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, AutowiringUserLookupComponent, CM2MarkingWorkflowServiceComponent, UserLookupComponent}

object AddMarkingWorkflowCommand {

	def apply(department:Department, academicYear: AcademicYear) =
		new AddMarkingWorkflowCommandInternal(department, academicYear)
			with ComposableCommand[CM2MarkingWorkflow]
			with AddMarkingWorkflowValidation
			with MarkingWorkflowDepartmentPermissions
			with AddMarkingWorkflowDescription
			with ModifyMarkingWorkflowState
			with AutowiringCM2MarkingWorkflowServiceComponent
			with AutowiringUserLookupComponent
}

class AddMarkingWorkflowCommandInternal(
	val department: Department,
	val academicYear: AcademicYear) extends CommandInternal[CM2MarkingWorkflow] with CreatesMarkingWorkflow {

	self: ModifyMarkingWorkflowState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

	markersA = JArrayList()
	markersB = JArrayList()

	def applyInternal(): CM2MarkingWorkflow = {
		val data = MarkingWorkflowData(department, workflowName, markersAUsers, markersBUsers, workflowType, Some(sampler))
		val workflow = createWorkflow(data)
		workflow.isReusable = true
		workflow.academicYear = academicYear
		cm2MarkingWorkflowService.save(workflow)
		workflow
	}
}

trait AddMarkingWorkflowValidation extends ModifyMarkingWorkflowValidation with StringUtils {

	self: ModifyMarkingWorkflowState with UserLookupComponent =>

	def validate(errors: Errors) {

		if (department.cm2MarkingWorkflows.exists(w => w.academicYear == academicYear && w.name == workflowName )) {
			errors.rejectValue("workflowName", "name.duplicate.markingWorkflow", Array(workflowName), null)
		}

		if (workflowType == null)
			errors.rejectValue("workflowType", "markingWorkflow.workflowType.none")
		else {
			rejectIfEmptyOrWhitespace(errors, "workflowName", "NotEmpty")
			markerValidation(errors, workflowType)
		}
	}
}

trait AddMarkingWorkflowDescription extends Describable[CM2MarkingWorkflow] {
	self: ModifyMarkingWorkflowState =>

	override lazy val eventName: String = "AddMarkingWorkflow"

	def describe(d: Description) {
		d.department(department)
	}
}