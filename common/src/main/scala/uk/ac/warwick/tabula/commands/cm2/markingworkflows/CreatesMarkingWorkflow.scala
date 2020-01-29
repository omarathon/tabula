package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowServiceComponent

trait CreatesMarkingWorkflow {
  self: CM2MarkingWorkflowServiceComponent with ModifyMarkingWorkflowState =>

  def createAndSaveSingleUseWorkflow(assignment: Assignment): Unit = {
    val data = CM2MarkingWorkflow.MarkingWorkflowData(
      department,
      s"${assignment.module.code.toUpperCase} ${assignment.name}",
      markersAUsers,
      markersBUsers,
      workflowType,
      Option(sampler)
    )
    val workflow = CM2MarkingWorkflow(data)
    workflow.isReusable = false
    workflow.academicYear = assignment.academicYear
    cm2MarkingWorkflowService.save(workflow)
    assignment.cm2MarkingWorkflow = workflow
  }
}
