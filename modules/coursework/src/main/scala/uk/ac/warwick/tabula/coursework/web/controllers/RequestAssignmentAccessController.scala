package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.web.bind.annotation.{ ModelAttribute, RequestMapping }
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.commands.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import collection.mutable
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/request-access"))
class RequestAssignmentAccessController extends CourseworkController {

	hideDeletedItems

	// clumsy way to prevent a user spamming admins with emails.
	var requestedAccess = mutable.Queue[Pair[String, String]]()

	@ModelAttribute def cmd(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = 
		new RequestAssignmentAccessCommand(module, assignment, user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: RequestAssignmentAccessCommand, @PathVariable("assignment") assignment: Assignment) = Redirect(Routes.assignment(mandatory(assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(user: CurrentUser, form: RequestAssignmentAccessCommand, @PathVariable("assignment") assignment: Assignment): Mav = {
		if (!user.loggedIn) {
			nope(form, assignment)
		} else {
			if (!alreadyEmailed(user, form, assignment)) {
				form.apply()
			}
	
			Redirect(Routes.assignment(assignment)).addObjects("requestedAccess" -> true)
		}
	}

	// if user+assignment is in the queue, they already sent an email recently so don't resend.
	// queue size is limited to 1000 so eventually they would be able to send again, but not rapidly.
	// They will still be able to send as many times as there are app JVMs (currently 2).
	def alreadyEmailed(user: CurrentUser, form: RequestAssignmentAccessCommand, assignment: Assignment): Boolean = {
		val key = (user.apparentId, assignment.id)
		if (requestedAccess contains key) {
			true
		} else {
			requestedAccess.enqueue(key)
			while (requestedAccess.size > 1000)
				requestedAccess.dequeue()
			false
		}
	}

}
