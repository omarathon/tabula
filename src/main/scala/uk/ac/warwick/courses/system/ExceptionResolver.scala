package uk.ac.warwick.courses.system
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Required
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletException
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.Ordered
import uk.ac.warwick.courses.web.controllers.Mav
import uk.ac.warwick.util.core.ExceptionUtils
import java.util.{Map => JMap}
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.courses.UserError

/**
 * Implements the Spring HandlerExceptionResolver SPI to catch all errors.
 * 
 * Errors not caught by Spring will be forwarded by the web.xml error handler to 
 * ErrorController which delegates to ExceptionResolver.doResolve(e), so all errors
 * should come here eventually.
 */
class ExceptionResolver extends HandlerExceptionResolver with Logging with Ordered {
	
	@Required @BeanProperty var defaultView:String =_
	
	/**
	 * If the interesting exception matches one of these exceptions then
	 * the given view name will be used instead of defaultView.
	 * 
	 * Doesn't check subclasses, the exception class has to match exactly.
	 */
	@Required @BeanProperty var viewMappings:JMap[String,String] = Map[String,String]()
	
	override def resolveException(request:HttpServletRequest, response:HttpServletResponse, obj:Any, e:Exception):ModelAndView = {
		doResolve(e)
	}
	
	/**
	 * Simpler interface for ErrorController to delegate to, which is called when an exception
	 * happens beyond Spring's grasp.
	 */
	def doResolve(e:Throwable) = {
		e match {
	      case exception:Throwable => handle(exception)
	      case _ => handleNull
	    }
	}
	
	private def handle(exception:Throwable) = {
		val mav = Mav(defaultView)
		
		val interestingException = ExceptionUtils.getInterestingThrowable(exception, Array( classOf[ServletException] ))
		
		mav.addObject("originalException", exception)
	    mav.addObject("exception", interestingException)
		
		// User errors should just be debug logged.
		interestingException match {
			case ie:UserError => if (debugEnabled) logger.debug("Error caught", interestingException)
			case _ => logger.error("Error caught", interestingException)
		}
	    
	    viewMappings.get(interestingException.getClass.getName) match {
			case view:String => mav.setViewName(view)
			case null => //keep defaultView
		}
	    
	    mav
	}
	
		
	private def handleNull = {
		logger.error("Unexpectedly tried to resolve a null exception!")
		Mav(defaultView)
	}
	
}