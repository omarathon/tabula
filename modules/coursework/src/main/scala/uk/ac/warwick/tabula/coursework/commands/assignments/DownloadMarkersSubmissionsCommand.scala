package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.actions.DownloadSubmissions
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.CurrentUser
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.ArrayList


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
class DownloadMarkersSubmissionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	mustBeLinked(assignment, module)
	PermissionsCheck(DownloadSubmissions(assignment))

	@BeanProperty var submissions:JList[Submission] = ArrayList()

	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]

	override def applyInternal(): RenderableZip = {
		submissions = assignment.getMarkersSubmissions(user.apparentUser)
		
		if (submissions.isEmpty) throw new ItemNotFoundException
		
		val zip = zipService.getSomeSubmissionsZip(submissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) {
		val downloads = assignment.getMarkersSubmissions(user.apparentUser)
		
		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties("submissionCount" -> downloads.size)
	}

}