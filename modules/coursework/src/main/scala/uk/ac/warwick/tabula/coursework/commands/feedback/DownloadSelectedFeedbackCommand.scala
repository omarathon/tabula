package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSelectedFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, assignment)
	
	var assignmentService = Wire[AssignmentService]
	var zipService = Wire[ZipService]
	var feedbackDao = Wire[FeedbackDao]
	
    var filename: String = _

    var students: JList[String] = JArrayList()
    
    var feedbacks: JList[Feedback] = _
    
    override def applyInternal(): RenderableZip = {
		if (students.isEmpty) throw new ItemNotFoundException

		feedbacks = for (
			uniId <- students;
			feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId) // assignmentService.getSubmissionByUniId(assignment, uniId)
		) yield feedback

        
        if (feedbacks.exists(_.assignment != assignment)) {
            throw new IllegalStateException("Feedbacks don't match the assignment")
        }
        val zip = zipService.getSomeFeedbacksZip(feedbacks)
        val renderable = new RenderableZip(zip)
        if (callback != null) callback(renderable)
        renderable
    }

	override def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(students)

	override def describeResult(d: Description) = d
		.assignment(assignment)
		.studentIds(students)
		.properties(
			"feedbackCount" -> Option(feedbacks).map(_.size).getOrElse(0))
}
