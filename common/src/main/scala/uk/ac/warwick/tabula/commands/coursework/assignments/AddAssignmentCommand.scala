package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.data.model.{Assignment, Module, WorkflowCategory}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions._

// TODO TAB-7991 - Nuke this
class AddAssignmentCommand(module: Module = null) extends ModifyAssignmentCommand(module) {

  PermissionCheck(Permissions.Assignment.Create, module)

  def assignment: Assignment = null

  def resitOnly: Boolean = false

  override def applyInternal(): Assignment = transactional() {
    val assignment = new Assignment(module)
    assignment.addDefaultFields()
    copyTo(assignment)
    if (assignment.cm2MarkingWorkflow != null) {
      assignment.workflowCategory = Option(WorkflowCategory.Reusable)
    } else {
      assignment.workflowCategory = Option(WorkflowCategory.NoneUse)
    }
    service.save(assignment)
    assignment
  }

  override def describeResult(d: Description, assignment: Assignment): Unit = d.assignment(assignment)

  override def describe(d: Description): Unit = d.module(module).properties(
    "name" -> name,
    "openDate" -> Option(openDate).map(_.toString()).orNull,
    "closeDate" -> Option(closeDate).map(_.toString()).orNull,
  )

}
