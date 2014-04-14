package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Notification, HeronWarningNotification, Heron, ScheduledNotification}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.ScheduledNotificationDao
import org.mockito.Mockito._

class ScheduledNotificationServiceTest extends TestBase with Mockito {

	val service = new ScheduledNotificationServiceImpl
	val dao =  mock[ScheduledNotificationDao]
	val notificationService = mock[NotificationService]
	service.dao = dao
	service.notificationService = notificationService

	val heron = new Heron()
	val sn1 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(1))
	val sn2 = new ScheduledNotification("HeronDefeat", heron, DateTime.now.minusDays(2))
	val sn3 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(3))

	val scheduledNotifications =  Seq(sn1, sn2, sn3)

	when (dao.notificationsToComplete) thenReturn (scheduledNotifications )

	@Test
	def generateNotifications() {
		val notification = service.generateNotification(sn1)

		notification.isInstanceOf[HeronWarningNotification] should be (true)
		notification.title should be("You all need to know. Herons would love to kill you in your sleep")
		notification.url should be ("/beware/herons")
		notification.urlTitle should be ("see how evil herons really are")
	}

	@Test
	def processNotifications() {
		service.processNotifications()

		verify(notificationService, times(3)).push(isA[Notification[_,_]])

		for(sn <- scheduledNotifications) {
			sn.completed should be (true)
		}
	}

}
