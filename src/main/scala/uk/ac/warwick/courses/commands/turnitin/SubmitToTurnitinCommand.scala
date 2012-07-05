package uk.ac.warwick.courses.commands.turnitin

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.services.turnitin._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.jobs.JobService
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.jobs.SubmitToTurnitinJob
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.apache.commons.io.FilenameUtils

/** 
 * Creates a job that submits the assignment to Turnitin.
 * 
 * Returns the job instance ID for status tracking.
 */
@Configurable
class SubmitToTurnitinCommand(@BeanProperty var user:CurrentUser) extends Command[String] {
	
	@BeanProperty var assignment: Assignment =_
	@BeanProperty var module: Module =_
	
	// empty constructor for Spring binding
	def this() = this(null)
	
	@Autowired var jobService:JobService =_
	
	def apply = jobService.add(Option(user), SubmitToTurnitinJob(assignment))
	
	def describe(d:Description) = d.assignment(assignment)
	
	def incompatibleFiles = assignment.submissions flatMap { _.allAttachments } filterNot Turnitin.validFileType
	
}