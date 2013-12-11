package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.commands.{Notifies, Command}
import uk.ac.warwick.tabula.jobs.{Job, NotifyingJob}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.data.model.Notification

trait NotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A, B](cmd: Command[A])(f: => A): A = cmd match {
		case ns: Notifies[A, B] => {
			val result = f
			for (notification <- ns.emit(result)) {
				notificationService.push(notification)
			}
			result
		}
		case _ => f
	}

	/**
	 * For edge cases where manual notifications need to be made outside commands.
	 * Use the command-triggered mixin above where possible for better type safety.
	 */
	def notify[A](notifications: Seq[Notification[A]]) {
		notifications.foreach { n => notificationService.push(n) }
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
