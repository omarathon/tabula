package uk.ac.warwick.tabula.coursework.commands.assignments
import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.CurrentUser


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
class DownloadMarkersSubmissionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)
	
	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]

	override def applyInternal(): RenderableZip = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser).getOrElse(
			throw new IllegalStateException("Cannot download submissions for assignments with no mark schemes")
		)
		
		if (submissions.isEmpty) throw new ItemNotFoundException
		
		val zip = zipService.getSomeSubmissionsZip(submissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) {
		val downloads = assignment.getMarkersSubmissions(user.apparentUser).getOrElse(Seq())
		
		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties("submissionCount" -> downloads.size)
	}

}