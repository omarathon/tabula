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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

@Aspect
class EventLoggingAspect extends EventHandling {
	
	@BeanProperty var listener:EventListener = _
	
	@Pointcut("execution(* uk.ac.warwick.courses.commands.Command.apply(..)) && target(callee)")
	def applyCommand(callee:Describable): Unit = {}
	
	@Around("applyCommand(callee)")
	def aroundApplyCommand(jp:ProceedingJoinPoint, callee:Describable):Any = 
		recordEvent(callee) { jp.proceed }
	
}
