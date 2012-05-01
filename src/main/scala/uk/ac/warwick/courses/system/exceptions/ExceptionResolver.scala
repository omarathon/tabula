package uk.ac.warwick.courses.system.exceptions
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Required
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletException
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.Ordered
import uk.ac.warwick.util.core.ExceptionUtils
import uk.ac.warwick.courses.JavaImports._
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.courses.UserError
import uk.ac.warwick.courses.web.Mav
import org.springframework.beans.factory.annotation.Autowired

/**
 * Implements the Spring HandlerExceptionResolver SPI to catch all errors.
 * 
 * Errors not caught by Spring will be forwarded by the web.xml error handler to 
 * ErrorController which delegates to ExceptionResolver.doResolve(e), so all errors
 * should come here eventually.
 */
class ExceptionResolver extends HandlerExceptionResolver with Logging with Ordered {
	
	@Required @BeanProperty var defaultView:String =_
	
	@Autowired var exceptionHandler:ExceptionHandler =_
	
	/**
	 * If the interesting exception matches one of these exceptions then
	 * the given view name will be used instead of defaultView.
	 * 
	 * Doesn't check subclasses, the exception class has to match exactly.
	 */
	@Required @BeanProperty var viewMappings:JMap[String,String] = Map[String,String]()
	
	override def resolveException(request:HttpServletRequest, response:HttpServletResponse, obj:Any, e:Exception):ModelAndView = {
		doResolve(e, Some(request)).toModelAndView
	}
	
	/**
	 * Resolve an exception outside of a request. Doesn't return a model/view.
	 */
	def resolveException(e:Exception) { doResolve(e) }
	
	/**
	 * Simpler interface for ErrorController to delegate to, which is called when an exception
	 * happens beyond Spring's grasp.
	 */
	def doResolve(e:Throwable, request:Option[HttpServletRequest]=None):Mav = {
		e match {
	      case exception:Throwable => handle(exception, request)
	      case _ => handleNull
	    }
	}
	
	/**
	 * Catch any exception in the given callback. Useful for wrapping some
	 * work that's done outside of a request, such as a scheduled task, because
	 * otherwise the exception will be only minimally logged by the scheduler.
	 */
	def reportExceptions[T](fn : =>T) = 
		try fn  
		catch { case throwable => handle(throwable, None); throw throwable } 
		
	
	private def handle(exception:Throwable, request:Option[HttpServletRequest]) = {
		val interestingException = ExceptionUtils.getInterestingThrowable(exception, Array( classOf[ServletException] ))

		val mav = Mav(defaultView,
			"originalException" -> exception,
			"exception" -> interestingException
		)
		
		val token = ExceptionTokens.newToken
		
		// handler will do logging, emailing
		try {
			exceptionHandler.exception(ExceptionContext(token, interestingException, request))
		} catch {
			// This is very bad and should never happen - but still try to avoid showing
			// a plain JBoss exception to the user.
			case e:Exception => logger.error("Exception handling exception!", e)
		}
		
	    viewMappings.get(interestingException.getClass.getName) match {
			case view:String => { mav.viewName = view }
			case null => //keep defaultView
		}
	    
	    mav
	}
		
	private def handleNull = {
		logger.error("Unexpectedly tried to resolve a null exception!")
		Mav(defaultView)
	}
	
}