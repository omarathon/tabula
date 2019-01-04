package uk.ac.warwick.tabula.data

import org.joda.time.DateTime
import org.junit.Before
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.{Fixtures, Mockito, PersistenceTestBase}

@Transactional
class ScheduledNotificationDaoTest extends PersistenceTestBase with Mockito {

	val dao = new ScheduledNotificationDaoImpl

	val staff = Fixtures.staff("1234567")
	val student = Fixtures.student("9876543")
	val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

	val meeting1 = new MeetingRecord
	meeting1.creator = staff

	val relationship = StudentRelationship(staff, relType, student, DateTime.now)
	meeting1.relationships = Seq(relationship)

	val meeting2 = new MeetingRecord
	meeting2.creator = staff
	meeting2.relationships = Seq(relationship)

	def testNotification(target: MeetingRecord, date: DateTime): ScheduledNotification[MeetingRecord] = {
		val sn = new ScheduledNotification[MeetingRecord]("meeting1Warning", target, date)
		sn
	}

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
		SSOUserType.userLookup = smartMock[UserLookupService]
		// hbm2ddl generates a swathe of conflicting foreign key constraints for entity_id, so ignore for this test
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate()
	}

	@Test def saveAndFetch() {

		val tomorrow = DateTime.now.plusDays(1)
		val notification = testNotification(meeting1, tomorrow)

		session.save(relType)
		session.save(relationship)
		session.save(meeting1)

		dao.getById("meeting1WarningNotification") should be (None)
		dao.save(notification)
		dao.getById(notification.id) should be (Option(notification))

		session.flush()
		session.clear()

		val retrievedNotification = dao.getById(notification.id).get.asInstanceOf[ScheduledNotification[MeetingRecord]]
		retrievedNotification.completed should be (false)
		retrievedNotification.scheduledDate should be (tomorrow)
		retrievedNotification.target should not be null
		retrievedNotification.target.entity should be(meeting1)

		retrievedNotification.completed = true
		dao.save(retrievedNotification)
		session.flush()
		session.clear()
		dao.getById(notification.id).get.asInstanceOf[ScheduledNotification[MeetingRecord]].completed should be (true)

		session.clear()
		session.delete(notification)
		session.delete(meeting1)
		session.flush()
	}

	@Test def scheduledNotifications() {
		session.save(relType)
		session.save(relationship)
		session.save(meeting1)
		session.save(meeting2)

		val n1 = testNotification(meeting1, DateTime.now.plusDays(1))
		val n2 = testNotification(meeting2, DateTime.now.plusDays(2))
		val n3 = testNotification(meeting1, DateTime.now.plusDays(3))
		val n4 = testNotification(meeting1, DateTime.now.plusDays(4))
		n4.completed = true

		val notifications = Seq(n1, n2, n3, n4)
		notifications.foreach(dao.save)

		session.flush()

		dao.getScheduledNotifications(meeting1) should be (Seq(n1, n3))
		dao.getScheduledNotifications(meeting2) should be (Seq(n2))

		session.clear()
		session.delete(meeting1)
		session.flush()
	}

	@Test def getNotificationsToComplete() {
		session.save(relType)
		session.save(relationship)
		session.save(meeting1)

		val n1 = testNotification(meeting1, DateTime.now.minusDays(2))
		n1.completed = true
		val n2 = testNotification(meeting1, DateTime.now.minusDays(1))
		val n3 = testNotification(meeting1, DateTime.now)
		val n4 = testNotification(meeting1, DateTime.now.plusDays(1))


		val notifications = Seq(n1, n2, n3, n4)
		notifications.foreach(dao.save)

		session.flush()
		session.clear()

		// FIXME make Scrollable.map() work properly so I don't have to do this silly list append
		val result = collection.mutable.ListBuffer[ScheduledNotification[_]]()
		dao.notificationsToComplete.take(100).foreach{ e =>
			result += e
		}

		result should be (Seq(n2,n3))

		session.clear()
		session.delete(meeting1)
		session.flush()
	}

}