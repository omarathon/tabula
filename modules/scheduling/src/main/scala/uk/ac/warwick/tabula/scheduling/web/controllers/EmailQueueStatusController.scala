package uk.ac.warwick.tabula.scheduling.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.EmailNotificationService
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import org.joda.time.{DateTime, Minutes}

@Controller
@RequestMapping(Array("/emails/nagios"))
class EmailQueueStatusController extends BaseController {

	var emailNotificationService = Wire[EmailNotificationService]

	@RequestMapping
	def status(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// Number of unsent emails in queue
		val unsentEmailCount = emailNotificationService.unemailedRecipientCount

		// How old (in minutes) is the oldest item in the queue?
		val oldestUnsentEmail =
			emailNotificationService.unemailedRecipients
				.take(1)
				.map { recipient: RecipientNotificationInfo =>
					Minutes.minutesBetween(recipient.notification.created, DateTime.now).getMinutes
				}
				.headOption
				.getOrElse(0)

		// The time of the notification for the last sent email
		val lastSent = emailNotificationService.recentRecipients(0, 1).headOption.map { _.notification.created.getMillis }.getOrElse(-1)

		val status = s"queueLength,${unsentEmailCount},oldestItemInMinutes,${oldestUnsentEmail},lastSentTime,${lastSent}"

		response.addHeader("Content-Type", "text/plain")
		response.addHeader("Content-Length", status.length.toString)
		response.getWriter().write(status)
	}

}
