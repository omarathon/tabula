package uk.ac.warwick.courses.helpers
import org.apache.log4j.Logger

trait Logging {
    val loggerName = this.getClass.getName
    lazy val logger = Logger.getLogger(loggerName)
    lazy val debugEnabled = logger.isDebugEnabled
}