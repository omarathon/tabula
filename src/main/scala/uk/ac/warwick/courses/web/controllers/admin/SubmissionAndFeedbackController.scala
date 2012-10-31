package uk.ac.warwick.courses.web.controllers.admin

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable.TreeSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.services.AuditEventIndexService
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.web.controllers.BaseController

@Configurable @Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/list"))
class SubmissionsAndFeedbackController extends BaseController {

    @Autowired var auditIndexService: AuditEventIndexService = _

    @RequestMapping(method = Array(GET, HEAD))
    def list(command: ListSubmissionsCommand) = {
        val (assignment, module) = (command.assignment, command.module)
        mustBeLinked(mandatory(command.assignment), mandatory(command.module))
        mustBeAbleTo(Participate(command.module))

        val enhancedSubmissions = command.apply()  // an "enhanced submission" is simply a submission with a Boolean flag to say whether it has been downloaded
        val hasOriginalityReport = enhancedSubmissions.exists(_.submission.hasOriginalityReport)
        val uniIdsWithSubmissionOrFeedback = assignment.getUniIdsWithSubmissionOrFeedback.toSeq.sorted
        
        val students = for (uniId <- uniIdsWithSubmissionOrFeedback) yield {
            var enhancedSubmissionForUniId = new SubmissionListItem(new Submission(), false)
            var feedbackForUniId = new Feedback()
        	
        	// lists
            val enhancedSubmissionsForUniId = enhancedSubmissions.filter(submissionListItem => submissionListItem.submission.universityId == uniId)
            val feedbacksForUniId = assignment.feedbacks.filter(feedback => feedback.universityId == uniId)

            if (enhancedSubmissionsForUniId.size() > 1) {
                throw new IllegalStateException("More than one SubmissionListItem (" + enhancedSubmissionsForUniId.size() + ") for " + uniId);
            }
            else if (enhancedSubmissionsForUniId.size() > 0) {
                enhancedSubmissionForUniId = enhancedSubmissionsForUniId.head
            }
            else {
            	enhancedSubmissionForUniId = new SubmissionListItem(new Submission(), false)
            }
            
            if (feedbacksForUniId.size() > 1) {
                throw new IllegalStateException("More than one Feedback for " + uniId);
            }
            else if (feedbacksForUniId.size() > 0) {
                feedbackForUniId = feedbacksForUniId.head
            }

            new Item(uniId, enhancedSubmissionForUniId, feedbackForUniId)
        }

        Mav("admin/assignments/submissionsandfeedback/list",
            "assignment" -> assignment,
            //"submissions" -> submissions,
            "students" -> students,
            "hasOriginalityReport" -> hasOriginalityReport)
            .crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
    }

    class Item(val uniId: String, val enhancedSubmission: SubmissionListItem, val feedback: Feedback) {
    }
}
