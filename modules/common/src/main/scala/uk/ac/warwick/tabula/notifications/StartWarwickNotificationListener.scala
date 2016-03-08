package uk.ac.warwick.tabula.notifications

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{Http, thread, url}
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.params.HttpConnectionParams
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Notification, NotificationWithTarget, ToEntityReference}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.NotificationListener
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import uk.ac.warwick.tabula.{DateFormats, HttpClientDefaults, JsonObjectMapperFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.existentials
import scala.util.parsing.json.JSON

trait StartWarwickNotificationListener extends NotificationListener with DisposableBean {
	self: StartWarwickPropertiesComponent with TextRendererComponent =>

	@transient var json = JsonObjectMapperFactory.instance

	private lazy val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, HttpClientDefaults.connectTimeout)
			HttpConnectionParams.setSoTimeout(getParams, HttpClientDefaults.socketTimeout)
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy() {
		http.shutdown()
	}

	private def toStartActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Option[Map[String, Any]] = {
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

				http.when(_ == 201)(httpRequest >:+ handler)
			}

			case None => Future.successful(None)
		}


	}

	override def listen(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
		val id = postActivity(notification)

		Await.result(id, HttpClientDefaults.socketTimeout.millis)
	}

}

@Component
class AutowiringStartWarwickNotificationListener extends StartWarwickNotificationListener
	with AutowiringStartWarwickPropertiesComponent
	with AutowiredTextRendererComponent

trait StartWarwickPropertiesComponent {
	def startApiHostname: String
	def startApiProviderId: String
	def startApiUsername: String
	def startApiPassword: String
}

trait AutowiringStartWarwickPropertiesComponent extends StartWarwickPropertiesComponent {
	var startApiHostname = Wire.property("${start.hostname}")
	var startApiProviderId = Wire.property("${start.provider_id}")
	var startApiUsername = Wire.property("${start.username}")
	var startApiPassword = Wire.property("${start.password}")
}