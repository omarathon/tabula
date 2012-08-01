package uk.ac.warwick.courses.commands

import uk.ac.warwick.courses
import assignments.EditAssignmentCommand
import courses.data.model.Assignment
import uk.ac.warwick.courses.TestBase

class EditAssignmentCommandTest extends TestBase {
  @Test def instantiate {
    val assignment = new Assignment()
    assignment.addDefaultFields
    assignment.members = null // simulate a slightly older assignment that has no initial linked group
    assignment.name = "Big Essay"
    assignment.occurrence = "A"
    assignment.commentField.get.value = "Instructions"

    val command = new EditAssignmentCommand(assignment)
    command.name should be ("Big Essay")
    command.occurrence should be ("A")
    command.members should be ('empty)
    command.comment should be ("Instructions")
  }
}