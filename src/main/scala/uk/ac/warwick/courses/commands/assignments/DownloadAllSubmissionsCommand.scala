package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
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
class DownloadAllSubmissionsCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var filename: String = _

	@Autowired var zipService: ZipService = _

	override def apply = {
		val zip = zipService.getAllSubmissionsZip(assignment)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(assignment.submissions.map(_.universityId))
		.properties(
			"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))

}