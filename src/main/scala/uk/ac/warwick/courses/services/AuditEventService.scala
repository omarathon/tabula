package uk.ac.warwick.courses.services

import java.io.StringWriter
import java.sql.Clob
import scala.collection.JavaConversions.asScalaBuffer
import org.codehaus.jackson.map.JsonMappingException
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonParseException
import org.hibernate.dialect.Dialect
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.ac.warwick.courses.data.Transactions._
import org.springframework.util.FileCopyUtils
import javax.annotation.Resource
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.events.Event
import org.springframework.transaction.annotation.Propagation._

trait AuditEventService {
	def getById(id: Long): Option[AuditEvent]
	def mapListToObject(array: Array[Object]): AuditEvent
	def unclob(any: Object): String
	def save(event: Event, stage: String): Unit
	def save(auditevent: AuditEvent): Unit
	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent]
	def listRecent(start: Int, count: Int): Seq[AuditEvent]
	def parseData(data: String): Option[Map[String, Any]]
	def getByEventId(eventId: String): Seq[AuditEvent]

	def addRelated(event: AuditEvent): AuditEvent
}

@Component
class AuditEventServiceImpl extends Daoisms with AuditEventService {

	@Autowired var json: ObjectMapper = _

	@Resource(name = "mainDatabaseDialect") var dialect: Dialect = _

	private val baseSelect = """select 
		eventdate,eventstage,eventtype,masquerade_user_id,real_user_id,data,eventid,id
		from auditevent a"""

	private val idSql = baseSelect + " where id = :id"

	private val eventIdSql = baseSelect + " where eventid = :id"

	// for viewing paginated lists of events
	private val listSql = baseSelect + """ order by eventdate desc """

	// for getting events newer than a certain date, for indexing
	private val indexListSql = baseSelect + """ 
					where eventdate > :eventdate and eventstage = 'before'
					order by eventdate asc """

	/**
	 * Get all AuditEvents with this eventId, i.e. all before/after stages
	 * that were part of the same action.
	 */
	def getByEventId(eventId: String): Seq[AuditEvent] = {
		val query = session.createSQLQuery(eventIdSql)
		query.setString("id", eventId)
		query.list.asInstanceOf[JList[Array[Object]]] map mapListToObject
	}

	def mapListToObject(array: Array[Object]): AuditEvent = {
		if (array == null) {
			null
		} else {
			val a = new AuditEvent
			a.eventDate = new DateTime(array(0))
			a.eventStage = array(1).toString
			a.eventType = array(2).toString
			a.masqueradeUserId = array(3).asInstanceOf[String]
			a.userId = array(4).asInstanceOf[String]
			a.data = unclob(array(5))
			a.eventId = array(6).asInstanceOf[String]
			a.id = toIdType(array(7))
			a
		}
	}

	def toIdType(any: Object): Long = any match {
		case n: Number => n.longValue
	}

	def unclob(any: Object): String = any match {
		case clob: Clob => FileCopyUtils.copyToString(clob.getCharacterStream)
		case string: String => string
		case null => ""
	}

	def getById(id: Long): Option[AuditEvent] = {
		val query = session.createSQLQuery(idSql)
		query.setLong("id", id)
		//		Option(query.uniqueResult.asInstanceOf[Array[Object]]) map mapListToObject map addRelated
		Option(mapListToObject(query.uniqueResult.asInstanceOf[Array[Object]])).map { addRelated }
	}

	def addParsedData(event: AuditEvent) = {
		event.parsedData = parseData(event.data)
	}

	def addRelated(event: AuditEvent) = {
		event.related = getByEventId(event.eventId)
		event
	}

	def save(event: Event, stage: String) {
		doSave(event, stage)
	}

	def save(auditEvent: AuditEvent) {
		doSave(auditEvent.toEvent, auditEvent.eventStage)
	}

	/**
	 * Saves the event in a separate transaction to the main one,
	 * so that it can be committed even if the main operation is
	 * rolling back.
	 */
	def doSave(event: Event, stage: String) {
		transactional(propagation = REQUIRES_NEW) {
			// Both Oracle and HSQLDB support sequences, but with different select syntax
			// TODO evaluate this and the SQL once on init
			val nextSeq = dialect.getSelectSequenceNextValString("auditevent_seq")

			val query = session.createSQLQuery("insert into auditevent " +
				"(id,eventid,eventdate,eventtype,eventstage,real_user_id,masquerade_user_id,data) " +
				"values(" + nextSeq + ", :eventid, :date,:name,:stage,:user_id,:masquerade_user_id,:data)")
			query.setString("eventid", event.id)
			query.setTimestamp("date", event.date.toDate)
			query.setString("name", event.name)
			query.setString("stage", stage)
			query.setString("user_id", event.realUserId)
			query.setString("masquerade_user_id", event.userId)
			if (event.extra != null) {
				val data = new StringWriter()
				json.writeValue(data, event.extra)
				query.setString("data", data.toString)
			}
			query.executeUpdate()
		}
	}

	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent] = {
		val query = session.createSQLQuery(indexListSql)
		query.setTimestamp("eventdate", date.toDate)
		query.setMaxResults(max)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject)
	}

	def listRecent(start: Int, count: Int): Seq[AuditEvent] = {
		val query = session.createSQLQuery(listSql)
		query.setFirstResult(start)
		query.setMaxResults(count)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject)
	}

	// parse the data portion of the AuditEvent
	def parseData(data: String): Option[Map[String, Any]] = try {
		Option(json.readValue(data, classOf[Map[String, Any]]))
	} catch {
		case e @ (_: JsonParseException | _: JsonMappingException) => None
	}

}