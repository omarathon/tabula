package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model._
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.actions.Participate
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.courses.Features

class SubmissionReportCommand {
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
} 

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/submissions-report"))
class SubmissionReportController extends BaseController {

	@Autowired var features:Features =_
	@Autowired var userLookup:UserLookupInterface =_
	
	@RequestMapping()
	def get(command:SubmissionReportCommand): Mav = {	
	   mustBeLinked(command.assignment, command.module)
	   mustBeAbleTo(Participate(command.module))
	   
	   val report = command.assignment.submissionsReport
	   val submissionOnly = report.submissionOnly.toList map userByWarwickId sortBy surname
	   val feedbackOnly   = report.feedbackOnly.toList   map userByWarwickId sortBy surname
	   val hasNoAttachments = report.withoutAttachments.toList   map userByWarwickId sortBy surname
	   val hasNoMarks = report.withoutMarks.toList   map userByWarwickId sortBy surname
	   
	   Mav("admin/assignments/submissionsreport",
	       "assignment" -> command.assignment,
	       "submissionOnly" -> submissionOnly,
	       "feedbackOnly" -> feedbackOnly,
	       "hasNoAttachments" -> hasNoAttachments,
	       "hasNoMarks" -> hasNoMarks,
	       "report" -> report).noLayoutIf(ajax)
	}
	
	def userByWarwickId(id:String) = userLookup.getUserByWarwickUniId(id)
	
	def surname(user:User) = user.getLastName
	
}