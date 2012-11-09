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

}