package uk.ac.warwick.tabula.coursework.commands.feedback

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
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.spring.Wire

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSelectedFeedbackCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

	var assignmentService = Wire.auto[AssignmentService]
	var zipService = Wire.auto[ZipService]
	var feedbackDao = Wire.auto[FeedbackDao]
	
    @BeanProperty var assignment: Assignment = _
    @BeanProperty var module: Module = _
    @BeanProperty var filename: String = _

    @BeanProperty var students: JList[String] = ArrayList()
    
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
