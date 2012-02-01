package uk.ac.warwick.courses.helpers
import org.apache.log4j.Logger

trait Logging {
    val loggerName = this.getClass.getName
    lazy val logger = Logger.getLogger(loggerName)
    lazy val debugEnabled = logger.isDebugEnabled
    
    /**
     * Logs a debug message, with the given arguments inserted into the
     * format placeholders in the message. Checks debugEnabled for you,
     * so no need to do that. 
     */
    def debug(message:String, arguments:Any*) =
    	if (debugEnabled) logger.debug(message format (arguments:_*))
 
    /**
     * For logging the result of a function without having to break it
     * out into multiple lines.
     */
    def debugResult[T](description:String, result:T) : T = {
    	debug("%s: %s", description, result)	
    	result
    }
    
}