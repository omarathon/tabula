package uk.ac.warwick.courses.events

import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.data.Daoisms
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import uk.ac.warwick.courses.commands.DescriptionImpl
import uk.ac.warwick.courses.commands.Description
import org.springframework.beans.factory.annotation.Autowired

class DatabaseEventListener extends EventListener with Daoisms {

	@Autowired var json:ObjectMapper =_
	
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
	
	def beforeCommand(event:Event) = save(event, "before")
	def afterCommand(event:Event, returnValue: Any) = save(event, "after")
	def onException(event:Event, exception: Throwable) = save(event, "error")

}