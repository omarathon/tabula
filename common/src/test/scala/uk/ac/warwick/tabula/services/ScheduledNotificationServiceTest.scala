package uk.ac.warwick.tabula.services

import org.hibernate.{Session, SessionFactory, Transaction}
import org.joda.time.DateTime
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{MockScrollableResults, ScheduledNotificationDao, Scrollable}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ScheduledNotificationServiceTest extends TestBase with Mockito {

	val service = new ScheduledNotificationServiceImpl
	val dao: ScheduledNotificationDao =  mock[ScheduledNotificationDao]
	val notificationService: NotificationService = mock[NotificationService]
	service.dao = dao
	service.notificationService = notificationService

	val sessionFactory: SessionFactory = mock[SessionFactory]
	val session: Session = mock[Session]
	val transaction: Transaction = mock[Transaction]

	sessionFactory.getCurrentSession() returns session
	sessionFactory.openSession() returns session
	session.beginTransaction() returns transaction

	service.sessionFactory = sessionFactory

	val staff = Fixtures.staff("1234567")
	val student = Fixtures.student("9876543")
	val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

	val meeting = new MeetingRecord
	meeting.creator = staff

	val relationship = StudentRelationship(staff, relType, student, DateTime.now)
	meeting.relationships = Seq(relationship)

	val sn1: ScheduledNotification[MeetingRecord] = new ScheduledNotification("HeronWarning", meeting, DateTime.now.minusDays(1))
	sn1.id = "sn1"

	val sn2: ScheduledNotification[MeetingRecord] = new ScheduledNotification("HeronDefeat", meeting, DateTime.now.minusDays(2))
	sn2.id = "sn2"

	val sn3: ScheduledNotification[MeetingRecord] = new ScheduledNotification("HeronWarning", meeting, DateTime.now.minusDays(3))
	sn3.id = "sn3"

	type Entity = MeetingRecord
	session.get(classOf[ScheduledNotification[Entity]], "sn1") returns sn1.asInstanceOf[ScheduledNotification[Entity]]
	session.get(classOf[ScheduledNotification[Entity]], "sn2") returns sn2.asInstanceOf[ScheduledNotification[Entity]]
	session.get(classOf[ScheduledNotification[Entity]], "sn3") returns sn3.asInstanceOf[ScheduledNotification[Entity]]

	val scheduledNotifications = Seq(sn1, sn2, sn3)
	//val itr = scheduledNotifications.iterator

	val scrollingScheduledNotifications = new MockScrollableResults(scheduledNotifications)

	when (dao.notificationsToComplete) thenReturn Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]](scrollingScheduledNotifications, session)

	@Test
	def generateNotifications() {
		val notification = service.generateNotification(sn1).get

		notification.isInstanceOf[HeronWarningNotification] should be (true)
		notification.title should be("You all need to know. Herons would love to kill you in your sleep")
		notification.url should be ("/beware/herons")
		notification.urlTitle should be ("see how evil herons really are")
	}

	@Test
	def processNotifications() {
		service.processNotifications()

		verify(session, times(3)).saveOrUpdate(isA[Notification[_,_]])

		for(sn <- scheduledNotifications) {
			sn.completed should be (true)
		}

		verify(transaction, times(3)).commit()
	}

}
