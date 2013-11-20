package uk.ac.warwick.tabula.services

import java.io.StringWriter
import java.sql.Clob
import scala.collection.JavaConversions.asScalaBuffer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.codehaus.jackson.JsonParseException
import org.hibernate.dialect.Dialect
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.util.FileCopyUtils
import javax.annotation.Resource
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.data.{SessionComponent, ExtendedSessionComponent, Daoisms}
import uk.ac.warwick.tabula.events.Event
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import org.jadira.usertype.dateandtime.joda.columnmapper.TimestampColumnDateTimeMapper
import org.joda.time.DateTimeZone

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
class AutowiringEventServiceImpl extends AuditEventServiceImpl with Daoisms

class AuditEventServiceImpl extends AuditEventService {
 this:SessionComponent=>

	var json: ObjectMapper = JsonObjectMapperFactory.instance

	@Resource(name = "mainDatabaseDialect") var dialect: Dialect = _

	private val baseSelect = """select 
		eventdate,eventstage,eventtype,masquerade_user_id,real_user_id,data,eventid,id
		from auditevent a"""
		
	private val DateIndex = 0
	private val StageIndex = 1
	private val TypeIndex = 2
	private val MasqueradeIdIndex = 3
	private val RealIdIndex = 4
	private val DataIndex = 5
	private val EventIdIndex = 6
	private val IdIndex = 7

	private val idSql = baseSelect + " where id = :id"

	private val eventIdSql = baseSelect + " where eventid = :id"

	// for viewing paginated lists of events
	private val listSql = baseSelect + """ order by eventdate desc """

	// for getting events newer than a certain date, for indexing
	private val indexListSql = baseSelect + """ 
					where eventdate > :eventdate and eventstage = 'before'
					order by eventdate asc """
	
	private val timestampColumnMapper = {
		val mapper = new TimestampColumnDateTimeMapper
		mapper.setDatabaseZone(DateTimeZone.forID("Europe/London"))
		mapper.setJavaZone(DateTimeZone.forID("Europe/London"))
		mapper
	}

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
			a.eventDate = timestampColumnMapper.fromNonNullValue(array(DateIndex).asInstanceOf[java.sql.Timestamp])
			a.eventStage = array(StageIndex).toString
			a.eventType = array(TypeIndex).toString
			a.masqueradeUserId = array(MasqueradeIdIndex).asInstanceOf[String]
			a.userId = array(RealIdIndex).asInstanceOf[String]
			a.data = unclob(array(DataIndex))
			a.eventId = array(EventIdIndex).asInstanceOf[String]
			a.id = toIdType(array(IdIndex))
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
			query.setTimestamp("date", timestampColumnMapper.toNonNullValue(event.date))
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
		query.setTimestamp("eventdate", timestampColumnMapper.toNonNullValue(date))
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