package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{Mockito, Fixtures, PersistenceTestBase}
import org.junit.{After, Before}
import uk.ac.warwick.tabula.data.model.{SSOUserType, HeronWarningNotification, Notification}
import uk.ac.warwick.tabula.services.UserLookupService
import org.springframework.test.context.transaction.BeforeTransaction
import org.springframework.transaction.annotation.Transactional

class NotificationDaoTest extends PersistenceTestBase with Mockito {

	val notificationDao = new NotificationDaoImpl

	@Before
	def setup() {
		notificationDao.sessionFactory = sessionFactory
		SSOUserType.userLookup = smartMock[UserLookupService]
		// hbm2ddl generates a swathe of conflicting foreign key constraints for entity_id, so ignore for this test
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate()
	}

	@After
	def teardown() {
		SSOUserType.userLookup = null
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY TRUE").executeUpdate()
	}

	@Transactional
	@Test def saveAndFetch {
			val agent = Fixtures.user()
			val group = Fixtures.smallGroup("Blissfully unaware group")
			val notification = Notification.init(new HeronWarningNotification, agent, Seq(group))
			notification.id = "heronWarningNotificaton"

			session.save(group)

			notificationDao.getById(notification.id) should be (None)
			notificationDao.save(notification)
			notificationDao.getById(notification.id) should be (Option(notification))

			session.flush()
			session.clear()

			val retrievedNotification = notificationDao.getById(notification.id).get.asInstanceOf[HeronWarningNotification]
			retrievedNotification.title should be ("Blissfully unaware group - You all need to know. Herons would love to kill you in your sleep")
			retrievedNotification.url should be ("/beware/herons")
			retrievedNotification.item.entity should be(group)
			retrievedNotification.content.template should be ("/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl")
	}
}
