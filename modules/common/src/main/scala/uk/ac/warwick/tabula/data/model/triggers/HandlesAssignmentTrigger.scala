package uk.ac.warwick.tabula.data.model.triggers

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.coursework.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.commands.coursework.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.userlookup.AnonymousUser

trait HandlesAssignmentTrigger extends Logging {

	@transient
	var jobService = Wire[JobService]

	@transient
	var features = Wire[Features]

	def assignment: Assignment

	def handleAssignment(universityIds: Seq[String]): Unit = {
		if (assignment.automaticallyReleaseToMarkers && assignment.hasWorkflow) {
			val releaseToMarkersCommand = ReleaseForMarkingCommand(assignment.module, assignment, new AnonymousUser)
			releaseToMarkersCommand.students = JArrayList(universityIds)
			releaseToMarkersCommand.confirm = true
			releaseToMarkersCommand.onBind(null)
			releaseToMarkersCommand.apply()
		}

		if (assignment.automaticallySubmitToTurnitin && features.turnitinSubmissions) {
			SubmitToTurnitinCommand(assignment.module, assignment).apply()
		}
	}

}
