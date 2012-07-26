package uk.ac.warwick.courses.web.controllers

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import javax.validation.Valid
import uk.ac.warwick.courses.actions.{Submit, View}
import uk.ac.warwick.courses.commands.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.courses.commands.assignments.SubmitAssignmentCommand
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.helpers.DateTimeOrdering.orderedDateTime
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.data.model.forms.Extension
import org.joda.time.DateTime

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends BaseController {
  
	hideDeletedItems
	
	@RequestMapping
	def viewModule(@PathVariable module:Module) = {
		mustBeAbleTo(View(mandatory(module)))
		Mav("submit/module", 
			"module"-> module,
			"assignments" -> module.assignments.filterNot{_.deleted}.sortBy{ _.closeDate }.reverse
			)
	}
	
}

@Configurable
@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/resend-receipt"))
class ResendSubmissionEmail extends AbstractAssignmentController {
	
	hideDeletedItems
	
	@RequestMapping(method=Array(GET,HEAD))
	def nope(form:SendSubmissionReceiptCommand) = Redirect( Routes.assignment(mandatory(form.assignment)) )
	
	@RequestMapping(method=Array(POST)) 
	def sendEmail(user:CurrentUser, form:SendSubmissionReceiptCommand) : Mav = {
		form.user = user
		mustBeLinked(mandatory(form.assignment),  mandatory(form.module))
		
		val submission = assignmentService.getSubmissionByUniId(form.assignment, user.universityId)
		val hasEmail = user.email.hasText
		val sent:Boolean = submission match {
			case Some(submission) if (submission.submitted) =>
				form.submission = submission
				form.apply()
			case None => false
		}
		Mav("submit/receipt", 
				"submission" -> submission,
				"module"-> form.module,
				"assignment" -> form.assignment,
				"sent" -> sent,
				"hasEmail" -> hasEmail
		)
		
	}
	
}

@Configurable
@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class AssignmentController extends AbstractAssignmentController {

	hideDeletedItems

	validatesWith{ (cmd:SubmitAssignmentCommand,errors) => cmd.validate(errors) }
	
	@ModelAttribute def form(@PathVariable("module") module:Module, @PathVariable("assignment") assignment:Assignment, user:CurrentUser) = {  
		val cmd = new SubmitAssignmentCommand(mandatory(assignment), user)
		cmd.module = module
		cmd
	}
	
	
	def checks(form:SubmitAssignmentCommand) = {
    val assignment = form.assignment
    val module = form.module
		mustBeLinked(mandatory(assignment),  mandatory(module))
    mustBeAbleTo(Submit(assignment)) // includes check for restricted submission.
	}
	
	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method=Array(HEAD, GET), params=Array("embedded"))
	def embeddedView(user:CurrentUser, form:SubmitAssignmentCommand, errors:Errors) = {
		view(user,form,errors).embedded
	}
	
	@RequestMapping(method=Array(HEAD, GET), params=Array("!embedded"))
	def view(user:CurrentUser, form:SubmitAssignmentCommand, errors:Errors) = {
		val assignment = form.assignment
		val module = form.module
		
		form.onBind
		checks(form)
		
		val feedback = checkCanGetFeedback(assignment, user)
		val submission = assignmentService.getSubmissionByUniId(assignment, user.universityId).filter{_.submitted}

    val isExtended = assignment.isWithinExtension(user.apparentId)
    val ext = assignment.extensions.filter(_.userId == user.apparentId)

    /*
		 * Submission values are an unordered set without any proper name, so
		 * match them up into an ordered sequence of pairs.
		 * 
		 * If a submission value is missing, the right hand is None.
		 * If any submission value doesn't match the assignment fields, it just isn't shown.
		 */
		val submissionValues = submission.map{ submission =>
			assignment.fields.map { field =>
				(field, submission.values.find(_.name == field.name))
			}
		}.getOrElse(Seq.empty)
		
		if (user.loggedIn) {
			Mav("submit/assignment",
				"module"-> module,
				"assignment" -> assignment,
				"feedback" -> feedback,
				"submission" -> submission,
				"justSubmitted" -> form.justSubmitted,
        "isExtended" -> isExtended,
        "extension" -> ext
			)
		} else {
			RedirectToSignin() 
		}
	}
	
	@Transactional
	@RequestMapping(method=Array(POST))
	def submit(@PathVariable module:Module, user:CurrentUser, @Valid form:SubmitAssignmentCommand, errors:Errors) = {
		val assignment = form.assignment
		val module = form.module
		form.onBind
		checks(form)
		if (errors.hasErrors || !user.loggedIn) {
			view(user,form,errors)
		} else {
			val submission = form.apply
			val sendReceipt = new SendSubmissionReceiptCommand(submission, user)
			sendReceipt.apply()
			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}
			
}