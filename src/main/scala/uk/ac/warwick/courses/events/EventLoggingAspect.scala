package uk.ac.warwick.courses.events
import java.util.{List => JList}
import scala.collection.JavaConversions._
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.JoinPoint
import uk.ac.warwick.courses.commands.Describable
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.AfterReturning
import scala.reflect.BeanProperty
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import uk.ac.warwick.courses.commands.DescriptionImpl
import uk.ac.warwick.courses.RequestInfo

@Aspect
class EventLoggingAspect {
	@BeanProperty var listener:EventListener = _
	
	@Pointcut("execution(* uk.ac.warwick.courses.commands.Command.apply(..)) && target(callee)")
	def applyCommand(callee:Describable): Unit = {}
	
	@Around("applyCommand(callee)")
	def aroundApplyCommand(jp:ProceedingJoinPoint, callee:Describable):Any = {
		val event = createEvent(callee)
		try {
			listener.beforeCommand(event)
			val result = jp.proceed
			listener.afterCommand(event, result)
			return result
		} catch {
			case e:Throwable => {
				listener.onException(event, e)
				throw e
			}
		}
	}
	
	def createEvent(describable:Describable) = {
		val description = new DescriptionImpl
		describable.describe(description)
		val (apparentId, realUserId) = getUser match {
			case Some(user) => (user.apparentId, user.realId)
			case None => (null, null)
		}
		new Event(
			describable.eventName,
			apparentId,
			realUserId,
			description.allProperties.toMap
		)
	}
	
	def getUser = RequestInfo.fromThread match {
		case Some(info) => Some(info.user)
		case None => None
	}
	
//	
//	@Before("applyCommand(callee)")
//    def beforeAdviceApplyCommand(callee: Describable) = listener.beforeCommand(callee)
//	
//    @AfterReturning(pointcut="applyCommand(callee)", returning="returnValue")
//    def startExecution(callee: Describable, returnValue:Any) = listener.afterCommand(callee, returnValue)
//    	
//    @AfterThrowing(pointcut="applyCommand(callee)", throwing="exception")
//    def startExecution(callee: Describable, exception:Throwable) = listener.onException(callee, exception)
}
