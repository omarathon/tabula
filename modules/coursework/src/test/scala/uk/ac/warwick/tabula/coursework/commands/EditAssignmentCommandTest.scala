package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.coursework.commands.assignments.EditAssignmentCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Module

class EditAssignmentCommandTest extends TestBase {
  @Test def instantiate {
    val assignment = new Assignment()
    assignment.addDefaultFields()
    assignment.module = new Module()
    assignment.members = null // simulate a slightly older assignment that has no initial linked group
    assignment.name = "Big Essay"
    assignment.commentField.get.value = "Instructions"

    val command = new EditAssignmentCommand(assignment.module, assignment)
    command.name should be ("Big Essay")
    command.members should be ('empty)
    command.comment should be ("Instructions")
  }
}