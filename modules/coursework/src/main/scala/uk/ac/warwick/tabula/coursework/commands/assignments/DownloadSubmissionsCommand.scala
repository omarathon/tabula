package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.spring.Wire


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
  var zipService = Wire.auto[ZipService]

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var filename: String = _

	@BeanProperty var submissions: JList[Submission] = ArrayList()


	override def applyInternal(): RenderableZip = {
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