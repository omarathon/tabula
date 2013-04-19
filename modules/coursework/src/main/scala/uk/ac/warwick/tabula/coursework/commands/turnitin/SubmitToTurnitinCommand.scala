package uk.ac.warwick.tabula.coursework.commands.turnitin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.services.turnitin._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.jobs.SubmitToTurnitinJob
import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.jobs.JobInstance

/**
 * Creates a job that submits the assignment to Turnitin.
 *
 * Returns the job instance ID for status tracking.
 */
class SubmitToTurnitinCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[JobInstance] {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.CheckForPlagiarism, assignment)

	var jobService = Wire.auto[JobService]

	def applyInternal() = jobService.add(Option(user), SubmitToTurnitinJob(assignment))

	def describe(d: Description) = d.assignment(assignment)

	def incompatibleFiles = assignment.submissions flatMap { _.allAttachments } filterNot Turnitin.validFileType

}