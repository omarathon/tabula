package uk.ac.warwick.tabula.coursework.commands.turnitin

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitin.Turnitin
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.coursework.SubmitToTurnitinJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.coursework.jobs.SubmitToTurnitinLtiJob

/**
 * Creates a job that submits the assignment to Turnitin.
 *
 * Returns the job instance ID for status tracking.
 */
class SubmitToTurnitinCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[JobInstance] {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.CheckForPlagiarism, assignment)

	var jobService = Wire[JobService]

	def applyInternal() = {
		if (features.turnitinLTI) jobService.add(Option(user), SubmitToTurnitinLtiJob(assignment))
		else jobService.add(Option(user), SubmitToTurnitinJob(assignment))
	}

	def describe(d: Description) = d.assignment(assignment)

	def incompatibleFiles = {
		val allAttachments = assignment.submissions.asScala.flatMap{ _.allAttachments }
		allAttachments.filterNot(Turnitin.validFileType) ++ allAttachments.filterNot(Turnitin.validFileSize)
	} 
	
}