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
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.ItemNotFoundException

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
@Configurable
class DownloadSubmissionsCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var filename: String = _

	@BeanProperty var submissions: JList[Submission] = ArrayList()

	@Autowired var zipService: ZipService = _

	override def work: RenderableZip = {
		if (submissions.isEmpty) throw new ItemNotFoundException
		if (submissions.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}
		val zip = zipService.getSomeSubmissionsZip(submissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.submissions(submissions)
		.studentIds(submissions.map(_.universityId))
		.properties(
			"submissionCount" -> Option(submissions).map(_.size).getOrElse(0))

}