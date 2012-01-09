package uk.ac.warwick.courses.services

import java.io.StringWriter
import java.sql.ResultSet
import java.util.{List => JList}
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

@Component
class AuditEventService extends Daoisms {

	@Autowired var json:ObjectMapper =_
	
	private val listSql = """select 
		eventdate,eventstage,eventtype,masquerade_user_id,real_user_id,data
		from auditevent a order by eventdate desc """
		
	val dialect = new Oracle10gDialect
	
	def mapListToObject(array:Array[Object]): AuditEvent = {
		val a = new AuditEvent
		a.eventDate = new DateTime(array(0))
		a.eventStage = array(1).toString
		a.eventType = array(2).toString
		a.masqueradeUserId = array(3).toString
		a.userId = array(4).toString
		a.data = array(5).toString
		a
	}
	
	def save(event:Event, stage:String) {
		val query = session.createSQLQuery("insert into auditevent " +
				"(eventdate,eventtype,eventstage,real_user_id,masquerade_user_id,data) " +
				"values(:date,:name,:stage,:user_id,:masquerade_user_id,:data)")
		query.setTimestamp("date", event.date.toDate)
		query.setString("name", event.name)
		query.setString("stage", stage)
		query.setString("user_id", event.realUserId)
		query.setString("masquerade_user_id", event.userId)
		if (event.extra != null) {
			val data = new StringWriter()
			json.writeValue(data, event.extra)
			query.setString("data", data.toString);
		}
		query.executeUpdate()
	}

	def listRecent(start:Int, count:Int):JList[AuditEvent] = {
		val jdbc = new JdbcTemplate(dataSource)
		val query = session.createSQLQuery(listSql)
		query.setFirstResult(start)
		query.setMaxResults(count)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject _)
	}
	
	
	
}