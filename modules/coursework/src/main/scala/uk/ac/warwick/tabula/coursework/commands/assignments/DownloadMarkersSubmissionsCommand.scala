package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.services.{StateService, AssignmentService, ZipService}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.CurrentUser
import reflect.BeanProperty
import uk.ac.warwick.tabula.JavaImports._


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
class DownloadMarkersSubmissionsCommand(
		val module: Module, 
		val assignment: Assignment, 
		val user: CurrentUser) 
		extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	var submissions:JList[Submission] = JArrayList()
	
	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]
	var stateService = Wire.auto[StateService]

	override def applyInternal(): RenderableZip = {
		submissions = assignment.getMarkersSubmissions(user.apparentUser)
		
		if (submissions.isEmpty) throw new ItemNotFoundException

		// update the state to downloaded for any marker feedback that exists.
		submissions.foreach{s =>
			assignment.feedbacks.find(_.universityId == s.universityId) match {
				case Some(f) if f.firstMarkerFeedback != null =>
					stateService.updateState(f.firstMarkerFeedback, InProgress)
				case _ => // do nothing
			}
		}

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