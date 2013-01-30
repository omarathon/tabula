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
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._


class DownloadAllSubmissionsCommand(val module: Module, val assignment: Assignment, val filename: String) extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read(), assignment)

	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
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