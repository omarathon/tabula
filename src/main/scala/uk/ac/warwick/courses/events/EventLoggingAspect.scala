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

@Aspect
class EventLoggingAspect {
	@BeanProperty var listener :EventListener = _
	
	@Pointcut("execution(* uk.ac.warwick.courses.commands.Command.apply(..)) && target(callee)")
	def applyCommand(callee:Describable): Unit = {}
	
	@Before("applyCommand(callee)")
    def beforeAdviceApplyCommand(callee: Describable) = listener.beforeCommand(callee)
	
    @AfterReturning(pointcut="applyCommand(callee)", returning="returnValue")
    def startExecution(callee: Describable, returnValue:Any) = listener.afterCommand(callee, returnValue)
    	
    @AfterThrowing(pointcut="applyCommand(callee)", throwing="exception")
    def startExecution(callee: Describable, exception:Throwable) = listener.onException(callee, exception)
}
