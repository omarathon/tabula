package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.commands.{Notifies, Command}
import uk.ac.warwick.tabula.jobs.{Job, NotifyingJob}

trait NotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A](cmd: Command[A])(f: => A): A = cmd match {
		case ns: Notifies[A] => {
			val result = f
			for (notification <- ns.emit) {
				notificationService.push(notification)
			}
			result
		}
		case _ => f
	}
}

trait JobNotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A](job: Job){
		job match {
			case ns:NotifyingJob[A] => for (notification <- ns.emit){
				notificationService.push(notification)
			}
			case _ => // do nothing. This job doesn't notify
		}
	}
}
