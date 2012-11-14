package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.web.bind.annotation.{ ModelAttribute, RequestMapping }
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.coursework.commands.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.tabula.coursework.web.{ Mav, Routes }
import uk.ac.warwick.tabula.CurrentUser
import collection.mutable

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/request-access"))
class RequestAssignmentAccessController extends AbstractAssignmentController {

	hideDeletedItems

	// clumsy way to prevent a user spamming admins with emails.
	var requestedAccess = mutable.Queue[Pair[String, String]]()

	@ModelAttribute def cmd(user: CurrentUser) = new RequestAssignmentAccessCommand(user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: RequestAssignmentAccessCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(user: CurrentUser, form: RequestAssignmentAccessCommand): Mav = {
		mustBeLinked(form.assignment, form.module)

		if (!alreadyEmailed(user, form)) {
			form.apply()
		}

		Redirect(Routes.assignment(form.assignment)).addObjects("requestedAccess" -> true)
	}

	// if user+assignment is in the queue, they already sent an email recently so don't resend.
	// queue size is limited to 1000 so eventually they would be able to send again, but not rapidly.
	// They will still be able to send as many times as there are app JVMs (currently 2).
	def alreadyEmailed(user: CurrentUser, form: RequestAssignmentAccessCommand): Boolean = {
		val key = (user.apparentId, form.assignment.id)
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
