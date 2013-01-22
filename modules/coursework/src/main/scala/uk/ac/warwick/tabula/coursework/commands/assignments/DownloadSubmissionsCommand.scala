package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.{StateService, ZipService, AssignmentService}
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.{DownloadedByMarker, Assignment, Module, Submission}
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.actions.Participate


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand(val module: Module, val assignment: Assignment) extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))
	
	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]
	var stateService = Wire.auto[StateService]

	@BeanProperty var filename: String = _
	@BeanProperty var submissions: JList[Submission] = ArrayList()
	@BeanProperty var students: JList[String] = ArrayList()

	override def applyInternal(): RenderableZip = {
		if (submissions.isEmpty && students.isEmpty) throw new ItemNotFoundException
		else if (!submissions.isEmpty && !students.isEmpty) throw new IllegalStateException("Only expecting one of students and submissions to be set")
		else if (!students.isEmpty && submissions.isEmpty) {
			submissions = for (
				uniId <- students;
				submission <- assignmentService.getSubmissionByUniId(assignment, uniId)
			) yield submission
		}
		
		if (submissions.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}

		// update the state to downloaded for any marker feedback that exists.
		submissions.foreach{s =>
			assignment.feedbacks.find(_.universityId == s.universityId) match {
				case Some(f) if f.firstMarkerFeedback != null =>
					stateService.updateState(f.firstMarkerFeedback, DownloadedByMarker)
				case _ => // do nothing
			}
		}

		val zip = zipService.getSomeSubmissionsZip(submissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) {

		val downloads: Seq[Submission] = {
			if (!students.isEmpty) students.flatMap(assignmentService.getSubmissionByUniId(assignment, _))
			else submissions
		}

		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties(
		"submissionCount" -> Option(downloads).map(_.size).getOrElse(0))
	}

}