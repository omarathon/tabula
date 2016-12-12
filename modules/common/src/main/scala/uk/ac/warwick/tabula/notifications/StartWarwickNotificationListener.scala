package uk.ac.warwick.tabula.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import dispatch.classic.url
import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{Notification, NotificationWithTarget, ToEntityReference}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AutowiringDispatchHttpClientComponent, DispatchHttpClientComponent, NotificationListener}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.existentials
import scala.util.parsing.json.JSON

trait StartWarwickNotificationListener extends NotificationListener {
	self: StartWarwickPropertiesComponent with TextRendererComponent with FeaturesComponent with DispatchHttpClientComponent =>

	@transient var json: ObjectMapper = JsonObjectMapperFactory.instance

	private def toStartActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Option[Map[String, Any]] = try {
		val recipients = notification.recipientNotificationInfos.asScala
			.filterNot(_.dismissed) // Not if the user has dismissed the notificaiton already
			.map(_.recipient)
			.filter(_.isFoundUser) // Only users found in SSO
			.map(_.getUserId)

		if (recipients.isEmpty) None
		else {
			val allEntities = notification match {
				case targetNotification: NotificationWithTarget[_, _] => targetNotification.items.asScala :+ targetNotification.target
				case _ => notification.items.asScala
			}

			val permissionsTargets = allEntities.map { _.entity }.collect { case pt: PermissionsTarget => pt }

			def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

			val tags = permissionsTargets.flatMap(withParents).distinct.map { target =>
				Map(
					"name" -> target.urlCategory,
					"value" -> target.urlSlug,
					"displayValue" -> target.humanReadableId
				)
			}

			Some(Map(
				"type" -> notification.notificationType,
				"title" -> notification.title,
				"text" -> textRenderer.renderTemplate(notification.content.template, notification.content.model),
				"generated_at" -> DateFormats.IsoDateTime.print(notification.created),
				"tags" -> tags,
				"recipients" -> Map(
					"users" -> recipients
				)
			))
		}
	} catch {
		// referenced entity probably missing, oh well.
		case e: ObjectNotFoundException => None
	}

	private def postActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Future[Option[String]] = {
		val endpoint = notification.notificationType match {
			case "SubmissionReceipt" => "activities"
			case _ => "notifications"
		}

		toStartActivity(notification) match {
			case Some(activity) => Future {
				val httpRequest = (url(
					s"https://$startApiHostname/api/streams/$startApiProviderId/$endpoint"
				) << (json.writeValueAsString(activity), "application/json")).as_!(startApiUsername, startApiPassword)

				def handler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
					req >- { (rawJSON) =>
						val response = JSON.parseFull(rawJSON) match {
							case Some(r: Map[String, Any] @unchecked) if r.get("success").contains(true) => r
							case _ => Map.empty[String, Any]
						}

						for {
							data <- response.get("data").collect { case r: Map[String, Any] @unchecked => r }
							id <- data.get("id").collect { case str: String => str }
						} yield id
					}
				}

				httpClient.when(_ == 201)(httpRequest >:+ handler)
			}

			case None => Future.successful(None)
		}


	}

	override def listen(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
		if (features.startNotificationListener) {
			val id = postActivity(notification)

			Await.result(id, 10.seconds)
		}
	}

}

@Component
class AutowiringStartWarwickNotificationListener extends StartWarwickNotificationListener
	with AutowiringStartWarwickPropertiesComponent
	with AutowiredTextRendererComponent
	with AutowiringFeaturesComponent
	with AutowiringDispatchHttpClientComponent

trait StartWarwickPropertiesComponent {
	def startApiHostname: String
	def startApiProviderId: String
	def startApiUsername: String
	def startApiPassword: String
}

trait AutowiringStartWarwickPropertiesComponent extends StartWarwickPropertiesComponent {
	var startApiHostname: String = Wire.property("${start.hostname}")
	var startApiProviderId: String = Wire.property("${start.provider_id}")
	var startApiUsername: String = Wire.property("${start.username}")
	var startApiPassword: String = Wire.property("${start.password}")
}