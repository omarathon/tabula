package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, Fixtures, TestBase}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Heron, Notification, NotificationPriority, HeronWarningNotification}
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.index.Term
import org.apache.lucene.search.{Sort, SortField, TermQuery}

import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}

class NotificationIndexServiceTest extends TestBase with Mockito {

	val service = new NotificationIndexServiceImpl {
		private val directory = new RAMDirectory()
		protected override def openDirectory() = directory
	}

	val dao = smartMock[NotificationDao]
	service.dao = dao

	// default behaviour, we add individual expectations later
	dao.getById(any[String]) returns None

	val agent = Fixtures.user(userId="abc")
	val recipient = Fixtures.user(userId="xyz")
	val otherRecipient = Fixtures.user(userId="xyo")
	val victim = Fixtures.user("heronVictim")
	val heron = new Heron(victim)

	val now = DateTime.now

	// Selection of notifications intended for a couple of different recipients
	lazy val items = for (i <- 1 to 100) yield {
		val notification = new HeronWarningNotification
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
		val notification = new HeronWarningNotification
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

	def indexTestItems() {
		service.indexItems(items)
	}

	@Test
	def indexItems() {

		indexTestItems()

		val sort = new Sort(new SortField(service.UpdatedDateField, SortField.Type.LONG, true))
		val result = service.search(new TermQuery(new Term("recipient", "xyz") ), sort)
		val dates = result.transform { doc =>
			service.documentValue(doc, "created")
		}

		dates.size should be (50)
		dates.head.toLong should be (recipientNotifications.map{_.notification.created}.max.getMillis)
	}

	@Test
	def missingRecipient() {
		val anonUser = new AnonymousUser()
		val notification = Notification.init(new HeronWarningNotification, agent, heron, heron)
		service.indexItems(Seq(new RecipientNotification(notification, anonUser)))
	}

	@Test
	def ignoreDismissed() {

		indexTestItems()
		service.indexItems(Seq(dismissedItem))

		val request = ActivityStreamRequest(user=recipient, max=100, pagination=None)
		service.userStream(request).items.size should be (50)

		val includeDismissed = ActivityStreamRequest(user=recipient, includeDismissed=true, max=100, pagination=None)
		service.userStream(includeDismissed).items.size should be (51)
	}

	@Test
	def userStream() {
		indexTestItems()
		val request = ActivityStreamRequest(user=recipient, max=20, pagination=None)
		val page1 = service.userStream(request)
		page1.items.size should be (20)

		page1.items.map{_.id} should be (expectedIds.reverse.slice(0, 20))

		val page2Pagination = new SearchPagination(token=page1.token, last=page1.last.get)
		val page2 = service.userStream(request.copy(pagination=Some(page2Pagination)))
		page2.items.size should be (20)

		page2.items.map{_.id} should be (expectedIds.reverse.slice(20, 40))
	}

	@Test
	def priorityFilteredUserStream() {

		indexTestItems()
		// show critical items only - should be 10 items
		val criticalRequest = ActivityStreamRequest(user=recipient, priority=0.75, max=20, pagination=None)
		val page = service.userStream(criticalRequest)
		page.items.size should be (10)
		page.items.map{_.id} should be (criticalIds.reverse)

		// show >= warning items only - should be 30 items
		val warningRequest = ActivityStreamRequest(user=recipient, priority=0.5, max=20, pagination=None)
		val page1 = service.userStream(warningRequest)
		page1.items.size should be (20)
		page1.items.map{_.id} should be (warningIds.reverse.slice(0, 20))

		val page2Pagination = new SearchPagination(token=page1.token, last=page1.last.get)
		val page2 = service.userStream(warningRequest.copy(pagination=Some(page2Pagination)))
		page2.items.size should be (10)
		page2.items.map{_.id} should be (warningIds.reverse.slice(20, 30))
	}

}
