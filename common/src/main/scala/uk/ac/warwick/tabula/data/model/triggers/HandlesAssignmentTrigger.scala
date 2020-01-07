package uk.ac.warwick.tabula.data.model.triggers

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.cm2.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.commands.cm2.turnitin.{SubmitToTurnitinCommand => CM2SubmitToTurnitinCommand}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.userlookup.AnonymousUser

trait HandlesAssignmentTrigger extends Logging {

  @transient
  var jobService: JobService = Wire[JobService]

  @transient
  var features: Features = Wire[Features]

  @transient
  var assessmentService: AssessmentService = Wire[AssessmentService]

  def assignment: Assignment

  def handleAssignment(usercodes: Seq[String]): Unit = {
    if (assignment.automaticallyReleaseToMarkers) {
      if (assignment.hasWorkflow) {
        val releaseToMarkersCommand = ReleaseForMarkingCommand(assignment, new AnonymousUser)
        releaseToMarkersCommand.students = JArrayList(usercodes)
        releaseToMarkersCommand.confirm = true
        releaseToMarkersCommand.apply()
      }
    }

    handleTurnitinSubmission()
  }

  def handleTurnitinSubmission(): Unit = {
    if (assignment.automaticallySubmitToTurnitin && features.turnitinSubmissions) {
      // TAB-4718
      val freshAssignment = assessmentService.getAssignmentById(assignment.id).get
      CM2SubmitToTurnitinCommand(freshAssignment).apply()
    }
  }
}
