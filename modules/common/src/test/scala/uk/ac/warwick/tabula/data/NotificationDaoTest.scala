package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions._

import org.junit.{Ignore, After, Before}
import org.springframework.test.context.transaction.{TransactionConfiguration, BeforeTransaction}
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.annotation.{ClassPathScanningCandidateComponentProvider, ClassPathBeanDefinitionScanner}
import org.springframework.core.`type`.filter.AssignableTypeFilter
import uk.ac.warwick.tabula.{PackageScanner, Mockito, Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, UserIdRecipientNotification, UniversityIdRecipientNotification, SSOUserType, HeronWarningNotification, Notification}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User
import scala.reflect.runtime.universe._
import javax.persistence.DiscriminatorValue
import org.joda.time.{DateTimeUtils, DateTime}

@Transactional
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
		DateTimeUtils.setCurrentMillisSystem()
	}

	@Test def saveAndFetch() {
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

	@Test def recent() {
		val agent = Fixtures.user()
		val group = Fixtures.smallGroup("Blissfully unaware group")
		session.save(group)
		var ii = 0
		val now = DateTime.now
		DateTimeUtils.setCurrentMillisFixed(now.getMillis)
		val notifications = for (i <- 1 to 1000) {
			val notification = Notification.init(new HeronWarningNotification, agent, Seq(group))
			notification.created = now.minusMinutes(i)
			notificationDao.save(notification)
			ii += 1
		}
		session.flush()

		val everything = notificationDao.recent().takeWhile(n => true).toSeq
		everything.size should be (1000)

		val oneHundred = notificationDao.recent().take(100).toSeq
		oneHundred.size should be (100)

		def noOlder(mins: Int)(n: Notification[_,_]) = n.created.isAfter(now.minusMinutes(mins))

		val recent = notificationDao.recent().takeWhile(noOlder(25)).toSeq
		recent.size should be (25)

	}

	/**
	 * Ensure there's nothing obviously wrong with the Notification subclass mappings. This will detect e.g.
	 * if an @Entity or @DiscriminatorValue are missing.
	 */
	@Test def nooneDied() {
		val notificationClasses = PackageScanner.subclassesOf[Notification[_,_]]("uk.ac.warwick.tabula.data.model")
		withClue("Package scanner should find a sensible number of classes") {
			notificationClasses.size should be > 5
		}

		val user = new User
		user.setUserId("testid")

		for (clazz <- notificationClasses) {
			try {
				val notification = clazz.getConstructor().newInstance().asInstanceOf[Notification[ToEntityReference,_]]
				notification.agent = user

				session.save(notification)

				if (clazz.getAnnotation(classOf[DiscriminatorValue]) == null) {
					fail(s"Notification ${clazz} has no @DiscriminatorValue annotation")
				}

				// FIXME we do want to flush because it would test things we care about, but many of the subclasses
				// expect specific properties to be set in order to save successfully. Need to do magic reflection to
				// work out what this is???
				//session.flush()
			} catch {
				case e: Exception => {
					fail(s"Exception saving ${clazz}", e)
				}
			}
		}
	}
}
