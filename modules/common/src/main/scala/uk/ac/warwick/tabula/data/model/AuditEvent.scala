package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.Id
import uk.ac.warwick.tabula.events.Event

/**
 * Represents a single item in the audit trail.
 * Every event has a unique ID, but an action will usually have
 * multiple events relating to each stage of the action. eventStage
 * stores the stage as "before", "after", "error". Events for the
 * same action are joined by the eventId which they share.
 *
 * It might make more sense to have a separate class responsible
 * for holding a group of these, rather than reusing the same class
 * to hold a reference to its siblings (and itself).
 *
 * AuditEventService.getById will fetch events with the same eventId
 * and place them in the "related" property. This list includes this
 * event itself.
 */
case class AuditEvent(
	var id: Long = 0, // unique to this object
	var eventId: String = null, // shared between different stage of the same action
	var eventDate: DateTime = null,
	var eventType: String = null,
	var eventStage: String = null, // before, after, error

	// The actual user who did the event.
	var userId: String = null,

	// Who appeared to do the event. If you're masqueraded as X, this field will contain X
	// while userId will contain your ID.
	var masqueradeUserId: String = null,

	//todo convert to/from json
	var data: String = null,

	/** this is set manually */
	var parsedData: Option[Map[String, Any]] = None,

	// list of other related events (with same eventId) manually set by DAO
	var related: Seq[AuditEvent] = Nil) {

	/** Collects up all the parsed data maps for all related events. */
	def relatedParsedData: Seq[Map[String, Any]] = related.flatMap { _.parsedData }

	/**
	 * Joins the JSON data for all the related audit events into one map. It is a simple
	 * merge by key - if any events re-use the same key, only one will get returned.
	 */
	def combinedParsedData = relatedParsedData.foldLeft(Map.empty[String, Any]) { (list, map) => map ++ list }

	def assignmentId = stringProperty("assignment")
	def submissionId = stringProperty("submission")
	def submissionIsNoteworthy = stringProperty("submissionIsNoteworthy")
	def submissionIds = stringListProperty("submissions")
	def feedbackIds = stringListProperty("feedbacks")

	/** Was there an "error" stage, indicating an exception was thrown? */
	def hadError = findStage("error").isDefined

	def findStage(stage: String) = related.find(_.eventStage == stage)
	def findBeforeStage = findStage("before")

	/** Returns whether any of the events have a property. */
	def hasProperty(name: String): Boolean = stringProperty(name).isDefined

	/** Returns whether any of the events have a property with this string value. */
	def hasProperty(name: String, value: String): Boolean =
		stringProperty(name)
			.map(_ == value)
			.getOrElse(false)

	/**
	 * Looks among the JSON data for a string value under this name.
	 * It checks the related events' JSON too, returning the first value
	 * found.
	 */
	private def stringProperty(name: String): Option[String] =
		relatedParsedData
			.flatMap { _.get(name) }
			.map { _.toString }.headOption

	private def stringListProperty(name: String): Seq[String] =
		relatedParsedData
			.flatMap { _.get(name) }
			.flatMap { _.asInstanceOf[collection.mutable.Buffer[String]] }

	/**
	 * Convert to an Event object (losing the stage information). There's usually
	 * little reason to do this except maybe during testing.
	 */
	def toEvent = Event(
		eventId,
		eventType,
		masqueradeUserId,
		userId,
		parsedData.getOrElse(Map.empty),
		eventDate)

}