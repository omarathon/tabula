package uk.ac.warwick.courses.events

import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.data.Daoisms
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import uk.ac.warwick.courses.commands.DescriptionImpl
import uk.ac.warwick.courses.commands.Description
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AuditEventService

class DatabaseEventListener extends EventListener with Daoisms {

	@Autowired var auditEventService:AuditEventService =_
	
	def save(event:Event, stage:String) {
		auditEventService.save(event, stage)
	}
	
	def beforeCommand(event:Event) = save(event, "before")
	def afterCommand(event:Event, returnValue: Any) = save(event, "after")
	def onException(event:Event, exception: Throwable) = save(event, "error")

}