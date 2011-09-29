package uk.ac.warwick.courses.helpers
import org.apache.log4j.Logger

trait Logging {
    val loggerName = this.getClass.getName
    lazy val logger = Logger.getLogger(loggerName)
    
    def logAround(msg:Any, f:()=>Unit) = {
      logger.debug("Before " + msg)
      f()
      logger.debug("After " + msg)
    }
    
    def logBefore(msg:Any, f:()=>Unit) = {
      logger.debug(msg)
      f()
    }
}