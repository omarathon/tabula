package uk.ac.warwick.courses.events
import java.io.StringWriter
import org.apache.log4j.Logger
import org.codehaus.jackson.map.ObjectMapper
import org.hibernate.SessionFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.stereotype.Service

@Service
class EventLogger extends Daoisms {
	
	var logger = Logger.getLogger("AUDIT")
	val json = new ObjectMapper
	
	def save(event:Event) {
		val query = session.createQuery("insert into events(date,name,user,realuser) values(:date,:name,:user,:realuser)")
		query.setDate("date", event.date.toDate)
		query.setString("name", event.name)
		query.setString("user", event.user)
		query.setString("realuser", event.realUser)
		query.setString("item", event.item)
		if (event.extra != null) {
			val data = new StringWriter
			json.writeValue(data, event.extra)
			query.setString("data", data.toString);
		}
		query.executeUpdate()
	}
}