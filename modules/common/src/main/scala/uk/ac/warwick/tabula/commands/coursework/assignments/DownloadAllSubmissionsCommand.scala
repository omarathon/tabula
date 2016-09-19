package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

import scala.collection.JavaConversions._

//FIXME don't need module as well as assignment here, post CM2 switchover
class DownloadAllSubmissionsCommand(
		val module: Module,
		val assignment: Assignment,
		val filename: String)
		extends Command[RenderableFile] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	var zipService = Wire.auto[ZipService]

	override def applyInternal() = zipService.getAllSubmissionsZip(assignment)

	override def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(assignment.submissions.map(_.universityId))
		.properties(
			"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))

}