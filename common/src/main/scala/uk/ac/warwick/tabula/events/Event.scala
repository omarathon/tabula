package uk.ac.warwick.tabula.events

import java.io.Serializable
import java.util.UUID

import org.joda.time.DateTime
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands.{Describable, DescriptionImpl, ReadOnly}
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Event is a transient object created by the event listener to
 * track the information we need to log. The class that gets
 * saved to database is [[uk.ac.warwick.tabula.data.model.AuditEvent]].
 */
case class Event(
	id: String,
	name: String,
	userId: String,
	realUserId: String,
	ipAddress: String,
	userAgent: String,
	readOnly: Boolean,
	extra: Map[String, Any],
	date: DateTime = DateTime.now
) extends Serializable

/**
 * Stores an Event with its stage (before, after, error).
 * This is mainly for serializing in maintenance mode, so that we can
 * read it back out
 */
case class EventAndStage(event: Event, stage: String) extends Serializable

object Event {
	def fromDescribable(describable: Describable[_]): Event = doFromDescribable(describable, None)
	def resultFromDescribable[A](describable: Describable[A], result: A, id: String): Event = doFromDescribable(describable, Some(result), id)

	private def doFromDescribable[A](describable: Describable[A], result: Option[A], id: String = null) = {
		val description = new DescriptionImpl

		result match {
			case Some(r) => describable.describeResult(description, r)
			case _ => describable.describe(description)
		}

		val (apparentId, realUserId) = getUser match {
			case Some(user) => (user.apparentId, user.realId)
			case None => (null, null)
		}

		val ipAddress = RequestInfo.fromThread.flatMap { _.ipAddress.maybeText }.orNull
		val userAgent = RequestInfo.fromThread.flatMap { _.userAgent.maybeText }.orNull

		val eventId = id match {
			case id: String => id
			case _ => UUID.randomUUID.toString
		}

		val readOnly = describable.isInstanceOf[ReadOnly]

		Event(
			eventId,
			describable.eventName,
			apparentId,
			realUserId,
			ipAddress,
			userAgent,
			readOnly,
			description.allProperties
		)
	}

	private def getUser = RequestInfo.fromThread.map { _.user }
}