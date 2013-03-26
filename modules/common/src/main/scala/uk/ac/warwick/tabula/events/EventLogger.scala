package uk.ac.warwick.tabula.events
import java.io.StringWriter
import org.apache.log4j.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.SessionFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.stereotype.Service

@Service
class EventLogger extends Daoisms {

	var logger = Logger.getLogger("AUDIT")

}