package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ScheduledNotificationService, NotificationService}
import uk.ac.warwick.tabula.commands.{SchedulesNotifications, Notifies, Command}
import uk.ac.warwick.tabula.jobs.{Job, NotifyingJob}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging

trait NotificationHandling extends Logging {

	var notificationService = Wire.auto[NotificationService]
	var scheduledNotificationService = Wire.auto[ScheduledNotificationService]

	def notify[A, B](cmd: Command[A])(f: => A): A = {

		val result = f

		cmd match {
			case ns: Notifies[A, B] =>
				for (notification <- ns.emit(result)) {
					notificationService.push(notification)
				}
			case _ =>
		}

		cmd match {
			case sn: SchedulesNotifications[A] =>

				scheduledNotificationService.removeInvalidNotifications(result)

				for (scheduledNotification <- sn.scheduledNotifications(result)) {
					if (scheduledNotification.scheduledDate.isBeforeNow) {
						logger.warn("ScheduledNotification generated in the past, ignoring: " + scheduledNotification)
					} else {
						scheduledNotificationService.push(scheduledNotification)
					}
				}
			case _ =>
		}

		result
	}

	/**
	 * For edge cases where manual notifications need to be made outside commands.
	 * Use the command-triggered mixin above where possible for better type safety.
	 */
	def notify[A](notifications: Seq[Notification[_, _]]) {
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
