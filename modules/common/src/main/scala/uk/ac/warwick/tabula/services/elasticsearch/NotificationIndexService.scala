package uk.ac.warwick.tabula.services.elasticsearch

import javax.persistence.DiscriminatorValue

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.AnalyzerDefinition
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.AutowiringNotificationDaoComponent
import uk.ac.warwick.tabula.services.RecipientNotification

object NotificationIndexService {
	implicit object RecipientNotificationIndexable extends ElasticsearchIndexable[RecipientNotification] {
		override def fields(item: RecipientNotification): Map[String, Any] = {
			val recipient = item.recipient
			val notification = item.notification

			val notificationType = notification.getClass.getAnnotation(classOf[DiscriminatorValue]).value()
			val priority = notification.priorityOrDefault

			Map(
				"notification" -> notification.id,
				"recipient" -> recipient.getUserId,
				"notificationType" -> notificationType,
				"priority" -> priority.toNumericalValue,
				"dismissed" -> notification.isDismissed(recipient),
				"created" -> DateFormats.IsoDateTime.print(notification.created)
			)
		}

		override def lastUpdatedDate(item: RecipientNotification): DateTime = item.notification.created
	}
}

@Service
class NotificationIndexService
	extends AbstractIndexService[RecipientNotification]
		with AutowiringNotificationDaoComponent
		with NotificationElasticsearchConfig {

	override implicit val indexable = NotificationIndexService.RecipientNotificationIndexable

	/**
		* The name of the index that this service writes to
		*/
	@Value("${elasticsearch.index.notifications.name}") var indexName: String = _

	final override val IncrementalBatchSize: Int = 5000

	override val UpdatedDateField: String = "created"

	override protected def listNewerThan(startDate: DateTime, batchSize: Int) =
		notificationDao.recent(startDate).take(batchSize).flatMap { notification =>
			try {
				notification.recipients.toList.map { user => new RecipientNotification(notification, user) }
			} catch {
				// Can happen if reference to an entity has since been deleted, e.g.
				// a submission is resubmitted and the old submission is removed. Skip this notification.
				case onf: ObjectNotFoundException =>
					debug("Skipping notification %s as a referenced object was not found", notification)
					Nil
			}
		}.filter { notification =>
			val recipient = notification.recipient
			recipient.isFoundUser && recipient.getUserId != null
		}
}

trait NotificationElasticsearchConfig extends ElasticsearchConfig {
	override def fields: Seq[TypedFieldDefinition] = Seq(
		doubleField("priority"),
		booleanField("dismissed"),
		dateField("created") format "strict_date_time_no_millis"
	)

	override def analysers: Seq[AnalyzerDefinition] = Seq() // default standard analyzer
}
