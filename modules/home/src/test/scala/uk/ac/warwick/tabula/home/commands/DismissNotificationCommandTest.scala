package uk.ac.warwick.tabula.home.commands

import uk.ac.warwick.tabula.{Mockito, Fixtures, TestBase}
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import uk.ac.warwick.tabula.services.{NotificationServiceComponent, NotificationService}

class DismissNotificationCommandTest extends TestBase with Mockito {

	val agent = Fixtures.user("7891011", "custrd")
	val recipient = Fixtures.user("0123456", "custrd")
	val n1 = Fixtures.notification(agent, recipient)
	val n2 = Fixtures.notification(recipient, recipient)
	val notifications = Seq(n1, n2)
	val ns = smartMock[NotificationService]

	trait CommandTestSupport extends NotificationServiceComponent {
		def notificationService = ns
	}

	@Test
	def dismissal() {
		val cmd = new DismissNotificationCommandInternal(notifications, dismiss=true, recipient) with CommandTestSupport
		cmd.applyInternal()
		notifications.foreach(n =>
			n.isDismissed(recipient) should be (true)
		)
		verify(ns, times(1)).update(notifications, recipient)
	}
}
