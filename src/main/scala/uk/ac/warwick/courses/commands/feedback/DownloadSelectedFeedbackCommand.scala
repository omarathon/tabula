package uk.ac.warwick.courses.commands.feedback

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
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.data.model.Feedback

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
@Configurable
class DownloadSelectedFeedbackCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

    @BeanProperty var assignment: Assignment = _
    @BeanProperty var module: Module = _
    @BeanProperty var filename: String = _

    @BeanProperty var students: JList[String] = ArrayList()
    
    @Autowired var zipService: ZipService = _
    @Autowired var assignmentService: AssignmentService = _
    @Autowired var feedbackDao: FeedbackDao = _
    
    var feedbacks: JList[Feedback] = _
    
    override def apply: RenderableZip = {
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
