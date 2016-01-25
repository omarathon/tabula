package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.testkit.{ElasticSugar, IndexMatchers}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.junit.After
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.{DateFormats, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

import scala.collection.JavaConverters._

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

		implicit val indexable = NotificationIndexService.IndexedNotificationIndexable
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
			IndexedNotification(notification, theRecipient)
		}

		// The IDs of notifications we expect our recipient to get.
		lazy val recipientNotifications = items.filter { _.recipient == recipient }
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }.await
	}

	@Test def fields(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val notification = new HeronDefeatedNotification
		notification.id = "defeat"
		notification.created = DateTime.now()
		notification.priority = NotificationPriority.Info

		val recipient = Fixtures.user(userId = "xyz")

		val item = IndexedNotification(notification, recipient)

		indexer.indexItems(Seq(item)).await
		blockUntilExactCount(1, indexName, indexName)

		// University ID is the ID field so it isn't in the doc source
		val doc = client.execute { get id item.id from indexName / indexName }.futureValue

		doc.source.asScala.toMap should be (Map(
			"notification" -> "defeat",
			"recipient" -> "xyz",
			"notificationType" -> "HeronDefeat",
			"priority" -> 0.25,
			"dismissed" -> false,
			"created" -> "2000-06-01T00:00:00+01:00"
		))
	}}

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
		indexer.indexItems(Seq(IndexedNotification(notification, anonUser))).await
		// No exception, good times.
	}

}
