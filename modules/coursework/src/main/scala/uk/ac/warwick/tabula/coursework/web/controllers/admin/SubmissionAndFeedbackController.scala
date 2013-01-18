package uk.ac.warwick.tabula.coursework.web.controllers.admin
import scala.collection.JavaConversions.seqAsJavaList

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AuditEventIndexService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/list"))
class SubmissionsAndFeedbackController extends CourseworkController {

	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]
	
	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment) = 
		new ListSubmissionsCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def list(command: ListSubmissionsCommand) = {
		val (assignment, module) = (command.assignment, command.module)

		// TODO This is too much logic for a controller. This should go in a command object, possibly nesting the ListSubmissionsCommand
		val enhancedSubmissions = command.apply()  // an "enhanced submission" is simply a submission with a Boolean flag to say whether it has been downloaded
		val hasOriginalityReport = enhancedSubmissions.exists(_.submission.hasOriginalityReport)
		val uniIdsWithSubmissionOrFeedback = assignment.getUniIdsWithSubmissionOrFeedback.toSeq.sorted
		val moduleMembers = assignmentService.determineMembershipUsers(assignment)		
		
		val awaitingSubmission = 
			if (moduleMembers == null) {
				Nil
			} else {
				val memberMap = Map() ++ ( for (member <- moduleMembers) yield (member.getWarwickId -> member.getFullName) )
				(memberMap -- uniIdsWithSubmissionOrFeedback).toList.sorted.toMap				
			}

		// later we may do more complex checks to see if this particular mark scheme workflow requires that feedback is released manually
		// for now all markschemes will require you to release feedback so if one exists for this assignment - provide it
		val mustReleaseForMarking = assignment.markScheme != null

		val students = for (uniId <- uniIdsWithSubmissionOrFeedback) yield {
			val usersSubmissions = enhancedSubmissions.filter(submissionListItem => submissionListItem.submission.universityId == uniId)
			val usersFeedback = assignment.fullFeedback.filter(feedback => feedback.universityId == uniId)
		
			val userFilter = moduleMembers.filter(member => member.getWarwickId() == uniId)
			val user = if(userFilter.isEmpty) {
				userLookup.getUserByWarwickUniId(uniId)
			} else {
				userFilter.head
			}
						
			val userFullName = user.getFullName()
			
			val enhancedSubmissionForUniId = usersSubmissions.toList match {
				case head :: Nil => head
				case head :: others => throw new IllegalStateException("More than one SubmissionListItem (" + usersSubmissions.size() + ") for " + uniId)
				case Nil => new SubmissionListItem(new Submission(), false)
			}
			
			if (usersFeedback.size() > 1) {
				throw new IllegalStateException("More than one Feedback for " + uniId);
			}
			
			val feedbackForUniId: Feedback = usersFeedback.headOption.orNull

			new Item(uniId, enhancedSubmissionForUniId, feedbackForUniId, userFullName)
		}
		
		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		val hasPublishedFeedback = students.exists { student => 
			student.feedback != null && student.feedback.checkedReleased
		}

		Mav("admin/assignments/submissionsandfeedback/list",
			"assignment" -> assignment,
			"students" -> students,
			"awaitingSubmission" -> awaitingSubmission,
			"whoDownloaded" -> auditIndexService.whoDownloadedFeedback(assignment),
			"hasPublishedFeedback" -> hasPublishedFeedback,
			"hasOriginalityReport" -> hasOriginalityReport,
			"mustReleaseForMarking" -> mustReleaseForMarking)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	// Simple object holder
	class Item(val uniId: String, val enhancedSubmission: SubmissionListItem, val feedback: Feedback, val fullName: String) 
	
}
