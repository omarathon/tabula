package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.commands.{Notifies, Command}
import uk.ac.warwick.tabula.jobs.{Job, NotifyingJob}
import uk.ac.warwick.tabula.services.jobs.JobInstance

trait NotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A, B](cmd: Command[A])(f: => A): A = cmd match {
		case ns: Notifies[A, B] => {
			val result = f
			for (notification <- ns.emit(f)) {
				notificationService.push(notification)
			}
			result
		}
		case _ => f
	}
}

trait JobNotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A](instance: JobInstance, job: Job) {
		job match {
			case ns: NotifyingJob[A] => for (notification <- ns.popNotifications(instance)) {
				notificationService.push(notification)
			}
			case _ => // do nothing. This job doesn't notify
		}
	}
}
