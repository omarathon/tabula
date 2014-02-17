package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.Notification
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

class RecipientNotification(val notification: Notification[_,_], val recipient: User) {
	def id = s"${notification.id}:${recipient.getUserId}}"
}

trait NotificationIndexService {
	def indexFrom(startDate: DateTime): Unit
}

/**
 */
//@Service
class NotificationIndexServiceImpl extends AbstractIndexService[RecipientNotification] with NotificationIndexService {
	var dao = Wire[NotificationDao]
	var userLookup = Wire[UserLookupService]

	@Value("${filesystem.index.notifications.dir}") override var indexPath: File = _

	override val IdField: String = "id"
	override val UpdatedDateField: String = "created"
	override val analyzer: Analyzer = createAnalyzer
	override val IncrementalBatchSize: Int = 5000
	override val MaxBatchSize: Int = 1000000

	private def createAnalyzer = new StandardAnalyzer(LuceneVersion)

	override protected def toItems(docs: Seq[Document]): Seq[RecipientNotification] =
		for {
			doc <- docs
			id <- documentValue(doc, "notification")
			userId <- documentValue(doc, "recipient")
			recipient <- Option(userLookup.getUserByUserId(userId))
			notification <- dao.getById(id)
		}	yield new RecipientNotification(notification, recipient)

	override protected def toDocuments(item: RecipientNotification): Seq[Document] = {
		val recipient = item.recipient
		val notification = item.notification
		val doc = new Document

		val notificationType = notification.getClass.getAnnotation(classOf[DiscriminatorValue]).value()

		doc.add(plainStringField("notification", notification.id))
		doc.add(plainStringField("recipient", recipient.getUserId))
		doc.add(plainStringField("notificationType", notificationType))
		doc.add(dateField(UpdatedDateField, notification.created))
		Seq(doc)
	}

	override protected def getId(item: RecipientNotification) = item.id

	override protected def getUpdatedDate(item: RecipientNotification) = item.notification.created

	override protected def listNewerThan(startDate: DateTime, batchSize: Int) =
		dao.recent(startDate).take(batchSize).flatMap { notification =>
			try {
				notification.recipients.map { user => new RecipientNotification(notification, user) }
			} catch {
				// Can happen if reference to an entity has since been deleted, e.g.
				// a submission is resubmitted and the old submission is removed. Skip this notification.
				case onf: ObjectNotFoundException => Nil
			}
		}

}
