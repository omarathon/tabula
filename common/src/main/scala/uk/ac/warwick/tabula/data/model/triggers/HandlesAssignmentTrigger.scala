package uk.ac.warwick.tabula.data.model.triggers

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.cm2.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.commands.coursework.assignments.OldReleaseForMarkingCommand
import uk.ac.warwick.tabula.commands.coursework.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.tabula.commands.cm2.turnitin.{ SubmitToTurnitinCommand => CM2SubmitToTurnitinCommand }

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
				val releaseToMarkersCommand = OldReleaseForMarkingCommand(assignment.module, assignment, new AnonymousUser)
				releaseToMarkersCommand.students = JArrayList(usercodes)
				releaseToMarkersCommand.confirm = true
				releaseToMarkersCommand.onBind(null)
				releaseToMarkersCommand.apply()
			} else if (assignment.hasCM2Workflow && !usercodes.isEmpty) {
				// check if there are any submissions at all -Students who do not submit work are not released automatically.
				val releaseToMarkersCommand = ReleaseForMarkingCommand(assignment, new AnonymousUser)
				releaseToMarkersCommand.students = JArrayList(usercodes)
				releaseToMarkersCommand.confirm = true
				releaseToMarkersCommand.apply()
			}
		}

		if (assignment.automaticallySubmitToTurnitin && features.turnitinSubmissions) {
			// TAB-4718
			val freshAssignment = assessmentService.getAssignmentById(assignment.id).get
			if(assignment.cm2Assignment) {
				CM2SubmitToTurnitinCommand(freshAssignment).apply()
			} else {
				SubmitToTurnitinCommand(assignment.module, freshAssignment).apply()
			}
		}
	}

}
