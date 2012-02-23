package uk.ac.warwick.courses.services

import java.io.StringWriter
import java.sql.ResultSet
import uk.ac.warwick.courses.JavaImports._
import org.codehaus.jackson.map.ObjectMapper
import org.hibernate.Session
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import uk.ac.warwick.courses.data.model.AuditEvent
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.events.Event
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.PreparedStatementCallback
import java.sql.PreparedStatement
import java.sql.Connection
import org.hibernate.dialect.Oracle10gDialect
import org.springframework.transaction.annotation.Transactional
import collection.JavaConversions._
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Clob
import org.springframework.util.FileCopyUtils
import org.hibernate.dialect.Dialect
import javax.annotation.Resource

@Component
class AuditEventService extends Daoisms {

	@Autowired var json:ObjectMapper =_
	
	@Resource(name="mainDatabaseDialect") var dialect:Dialect = _
	
	private val baseSelect = """select 
		eventdate,eventstage,eventtype,masquerade_user_id,real_user_id,data,eventid
		from auditevent a"""
	
	// for viewing paginated lists of events
	private val listSql = baseSelect + """ order by eventdate desc """
	
	// for getting events newer than a certain date, for indexing
	private val indexListSql = baseSelect + """ 
					where eventdate >= :oldest
					order by eventdate asc """
	
	
		
	
	def mapListToObject(array:Array[Object]): AuditEvent = {
		val a = new AuditEvent
		a.eventDate = new DateTime(array(0))
		a.eventStage = array(1).toString
		a.eventType = array(2).toString
		a.masqueradeUserId = array(3).asInstanceOf[String]
		a.userId = array(4).asInstanceOf[String]
		a.data = unclob(array(5))
		a.eventId = array(6).asInstanceOf[String]
		a
	}
	
	def unclob(any:Object): String = any match {
		case clob:Clob => FileCopyUtils.copyToString(clob.getCharacterStream)
		case string:String => string
		case null => ""
	}
	
	def save(event:Event, stage:String) {
		// Both Oracle and HSQLDB support sequences, but with different select syntax
		// TODO evaluate this and the SQL once on init
		val nextSeq = dialect.getSelectSequenceNextValString("auditevent_seq")
		
		val query = session.createSQLQuery("insert into auditevent " +
				"(id,eventid,eventdate,eventtype,eventstage,real_user_id,masquerade_user_id,data) " +
				"values("+nextSeq+", :eventid, :date,:name,:stage,:user_id,:masquerade_user_id,:data)")
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
	
	def listNewerThan(date:DateTime, max:Int) : JList[AuditEvent] = {
		val query = session.createSQLQuery(indexListSql)
		query.setTimestamp("eventdate", date.toDate)
		query.setMaxResults(max)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject _)
	}

	def listRecent(start:Int, count:Int) : JList[AuditEvent] = {
		val query = session.createSQLQuery(listSql)
		query.setFirstResult(start)
		query.setMaxResults(count)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject _)
	}
	
	
	
}