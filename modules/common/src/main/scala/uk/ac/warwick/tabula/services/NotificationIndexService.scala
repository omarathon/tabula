package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{NotificationPriority, Notification}
import java.io.File
import org.apache.lucene.analysis.Analyzer
import org.joda.time.DateTime
import org.apache.lucene.document.Document
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.spring.Wire
import org.apache.lucene.analysis.standard.StandardAnalyzer
import uk.ac.warwick.userlookup.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.hibernate.ObjectNotFoundException
import javax.persistence.DiscriminatorValue
import org.apache.lucene.search._
import org.apache.lucene.index.{IndexWriterConfig, Term}
import uk.ac.warwick.tabula.JavaImports._
import org.apache.lucene.index.sorter.{SortingMergePolicy, NumericDocValuesSorter, Sorter}

class RecipientNotification(val notification: Notification[_,_], val recipient: User) {
	def id = s"${notification.id}-${recipient.getUserId}"
}

trait NotificationIndexService {
	def indexFrom(startDate: DateTime): Unit
	def incrementalIndex(): Unit
}

trait NotificationQueryMethods { self: NotificationIndexServiceImpl =>
	def userStream(req: ActivityStreamRequest): PagingSearchResultItems[Notification[_,_]] = {
		val user = req.user

		val recipientQuery = new TermQuery(new Term("recipient", user.getUserId))
		val priorityLimit = NumericRangeQuery.newDoubleRange("priority", req.priority, 1.0, true, true)

		val query = new BooleanQuery
		query.add(recipientQuery, BooleanClause.Occur.MUST)
		query.add(priorityLimit, BooleanClause.Occur.MUST)

		if (!req.includeDismissed) {
			val dismissedQuery = new TermQuery(new Term("dismissed", "false"))
			query.add(dismissedQuery, BooleanClause.Occur.MUST)
		}

		req.types.foreach { types =>
			val typesQuery = new BooleanQuery()
			types.foreach { typ =>
				typesQuery.add(new TermQuery(new Term("notificationType", typ)), BooleanClause.Occur.SHOULD)
			}
			query.add(typesQuery, BooleanClause.Occur.MUST)
		}

		val sort = new Sort(new SortField(UpdatedDateField, SortField.Type.LONG, true))
		val fieldDoc = req.pagination.map { p => new FieldDoc(p.lastDoc, Float.NaN, Array(p.lastField:JLong)) }
		val token = req.pagination.map { _.token }
		val result = search(query, req.max, sort, fieldDoc, token)
		result.transformAll[Notification[_,_]] {
			docs => docs.flatMap(toNotification)
		}
	}
}

/**
 */
@Service
class NotificationIndexServiceImpl extends AbstractIndexService[RecipientNotification] with NotificationIndexService with NotificationQueryMethods {
	override val loggerName = classOf[NotificationIndexService].getName

	var dao = Wire[NotificationDao]
	var userLookup = Wire[UserLookupService]

	@Value("${filesystem.index.notifications.dir}") override var indexPath: File = _

	override val IdField: String = "id"
	override val UpdatedDateField: String = "created"
	override val analyzer: Analyzer = createAnalyzer
	override val IncrementalBatchSize: Int = 5000
	override val MaxBatchSize: Int = 1000000

	private def createAnalyzer = new StandardAnalyzer(IndexService.NotificationIndexLuceneVersion)

	protected def toNotification(doc: Document): Option[Notification[_,_]] =
		for {
			id <- documentValue(doc, "notification")
			notification <- dao.getById(id)
		}	yield notification

	override protected def toItems(docs: Seq[Document]): Seq[RecipientNotification] =
		for {
			doc <- docs
			notification <- toNotification(doc)
			userId <- documentValue(doc, "recipient")
			recipient <- Option(userLookup.getUserByUserId(userId))
		}	yield new RecipientNotification(notification, recipient)

	override protected def toDocuments(item: RecipientNotification): Seq[Document] = {
		val recipient = item.recipient
		val notification = item.notification
		val doc = new Document

		if (recipient.isFoundUser && recipient.getUserId != null) {
			val notificationType = notification.getClass.getAnnotation(classOf[DiscriminatorValue]).value()
			val priority = notification.priorityOrDefault
			doc.add(plainStringField(IdField, item.id))
			doc.add(plainStringField("notification", notification.id))
			doc.add(plainStringField("recipient", recipient.getUserId))
			doc.add(plainStringField("notificationType", notificationType))
			doc.add(doubleField("priority", priority.toNumericalValue))
			doc.add(booleanField("dismissed", notification.isDismissed(recipient)))
			doc.add(dateField(UpdatedDateField, notification.created))
			// Index date as a DocValue so we can do efficient sorts on it.
			doc.add(docValuesField(UpdatedDateField, notification.created.getMillis))
			Seq(doc)
		} else {
			debug("Skipping RecipientNotification because foundUser=%b and userId=%s", recipient.isFoundUser, recipient.getUserId)
			Nil
		}
	}

	override protected def getId(item: RecipientNotification) = item.id

	override protected def getUpdatedDate(item: RecipientNotification) = item.notification.created

	override protected def listNewerThan(startDate: DateTime, batchSize: Int) =
		dao.recent(startDate).take(batchSize).flatMap { notification =>
			try {
				notification.recipients.toList.map { user => new RecipientNotification(notification, user) }
			} catch {
				// Can happen if reference to an entity has since been deleted, e.g.
				// a submission is resubmitted and the old submission is removed. Skip this notification.
				case onf: ObjectNotFoundException =>
					debug("Skipping notification %s as a referenced object was not found", notification)
					Nil
			}
		}

	/**
	 * Set merge policy so that when segments of index are merged, it sorts them
	 * by descending docvalue (date) at the same time. Then a correctly configured
	 * search query can more efficiently look by the same sort
	 */
	override def configureIndexWriter(config: IndexWriterConfig) {
		val sorter: Sorter = new NumericDocValuesSorter(UpdatedDateField, false)
		val sortingMergePolicy = new SortingMergePolicy(config.getMergePolicy, sorter)
		config.setMergePolicy(sortingMergePolicy)
	}

}
