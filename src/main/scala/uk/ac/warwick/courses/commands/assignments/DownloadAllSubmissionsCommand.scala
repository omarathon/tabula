package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.services.ZipService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Configurable

@Configurable
class DownloadAllSubmissionsCommand extends Command[RenderableZip] {

	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
	@BeanProperty var filename:String =_
	
	@Autowired var zipService:ZipService =_
	
	/**
	 * Optionally do some work for the handler inside the command,
	 * e.g. actually serving the file so that the audit logging is
	 * wrapped around the whole operation.
	 */
	var renderableHandler: (RenderableZip)=>Unit =_
	
	override def apply = {
		val zip = zipService.getAllSubmissionZips(assignment)
		val renderable = new RenderableZip(zip)
		if (renderableHandler != null) renderableHandler(renderable)
		renderable
	}
	
	override def describe(d:Description) = d.assignment(assignment).properties(
			"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0)
	)
	
}