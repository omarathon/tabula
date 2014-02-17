package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Fixtures, TestBase}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Notification, HeronWarningNotification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.index.{Fields, IndexReaderContext, Term, StoredFieldVisitor, IndexReader}
import org.apache.lucene.search.TermQuery
import javax.mail.search.StringTerm
import javax.mail.Message


class NotificationIndexServiceTest extends TestBase {

	val service = new NotificationIndexServiceImpl {
		private val directory = new RAMDirectory()
		protected override def openDirectory() = directory
	}

	val agent = Fixtures.user(userId="abc")
	val recipient = Fixtures.user(userId="xyz")
	val group = new SmallGroup

	@Test
	def indexItems() {

		val now = DateTime.now

		val notification = Notification.init(new HeronWarningNotification, agent, group)
		notification.id = "nid"
		notification.created = now

		val items = Seq(
			new RecipientNotification(notification, recipient)
		)
		service.indexItems(items)

		val result = service.search(new TermQuery(new Term("recipient", "xyz") ))
		val dates = result.transform { doc =>
			service.documentValue(doc, "created")
		}
		dates.size should be (1)
		dates.head.toLong should be (now.getMillis)
	}

}
