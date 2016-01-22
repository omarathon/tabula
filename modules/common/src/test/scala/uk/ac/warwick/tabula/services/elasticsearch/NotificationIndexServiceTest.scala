package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.testkit.{ElasticSugar, IndexMatchers}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.junit.After
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.services.RecipientNotification
import uk.ac.warwick.tabula.{DateFormats, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

class NotificationIndexServiceTest extends TestBase with Mockito with ElasticSugar with IndexMatchers {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	val indexName = "notification"

	private trait Fixture {
		val dao = smartMock[NotificationDao]

		// default behaviour, we add individual expectations later
		dao.getById(any[String]) returns None

		val indexer = new NotificationIndexService
		indexer.indexName = NotificationIndexServiceTest.this.indexName
		indexer.client = NotificationIndexServiceTest.this.client
		indexer.notificationDao = dao

		// Creates the index
		indexer.afterPropertiesSet()

		implicit val indexable = NotificationIndexService.RecipientNotificationIndexable
	}

	private trait DataFixture extends Fixture {
		val agent = Fixtures.user(userId="abc")
		val recipient = Fixtures.user(userId="xyz")
		val otherRecipient = Fixtures.user(userId="xyo")
		val victim = Fixtures.user("heronVictim")
		val heron = new Heron(victim)

		val now = DateTime.now

		// Selection of notifications intended for a couple of different recipients
		lazy val items = for (i <- 1 to 100) yield {
			val notification =
				if (i % 2 == 0) {
					new HeronWarningNotification
				} else {
					new HeronDefeatedNotification
				}
			notification.id = "nid"+i
			notification.created = now.plusMinutes(i)
			dao.getById(notification.id) returns Some(notification)

			notification.priority = if (i <= 40) {
				NotificationPriority.Info
			} else if (i <= 80) {
				NotificationPriority.Warning
			} else {
				NotificationPriority.Critical
			}

			val theRecipient = if (i % 2 == 0) {
				recipient
			} else {
				otherRecipient
			}
			new RecipientNotification(notification, theRecipient)
		}

		lazy val dismissedItem = {
			val heron2 = new Heron(recipient)
			val notification = Notification.init(new HeronWarningNotification, agent, heron2)
			notification.id = "nid101"
			notification.created = now.plusMinutes(101)
			dao.getById(notification.id) returns Some(notification)
			notification.dismiss(recipient)
			new RecipientNotification(notification, recipient)
		}

		// The IDs of notifications we expect our recipient to get.
		lazy val recipientNotifications = items.filter{_.recipient == recipient}
		lazy val expectedIds = recipientNotifications.map{_.notification.id}
		lazy val criticalIds = recipientNotifications.filter {_.notification.priority == Critical}.map{_.notification.id}
		lazy val warningIds =
			recipientNotifications.filter {i => i.notification.priority == Warning || i.notification.priority == Critical}
				.map{_.notification.id}
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }.await
	}

	@Test def indexItems(): Unit = new DataFixture {
		indexer.indexItems(items)
		blockUntilExactCount(100, indexName, indexName)

		val dates = client.execute {
			search in indexName / indexName query termQuery("recipient", "xyz") sort(field sort "created" order SortOrder.DESC ) limit 100
		}.map { _.hits.map { hit => DateFormats.IsoDateTime.parseDateTime(hit.sourceAsMap("created").toString) }.toSeq }
		  .futureValue

		dates.size should be (50)
		dates.head should be (recipientNotifications.map { _.notification.created.withMillisOfSecond(0) }.max)
	}

	@Test def missingRecipient(): Unit = new DataFixture {
		val anonUser = new AnonymousUser()
		val notification = Notification.init(new HeronWarningNotification, agent, heron)
		indexer.indexItems(Seq(new RecipientNotification(notification, anonUser))).await
		// No exception, good times.
	}

}
