package uk.ac.warwick.courses.commands

import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.events.EventHandling
import uk.ac.warwick.courses.services.MaintenanceModeService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.services.MaintenanceModeEnabledException
import uk.ac.warwick.courses.services.MaintenanceModeEnabledException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/** This is an AspectJ Aspect that matches calls to `apply()` on `Command` objects,
  * and calls the event handlers.
  * 
  * In production the Ant build weaves this aspect code in to the compiled classes,
  * so no special runtime work is needed.
  * 
  * When running in Eclipse, load time weaving is used which relies on running with
  * the aspectjweaver.jar agent, which reads our aop.xml to weave classes when they
  * are loaded into the JVM.
  */
@Configurable 
@Aspect
class CommandApplyAspect extends EventHandling {
	
	@BeanProperty @Autowired var maintenanceMode:MaintenanceModeService =_
	
	/**
	 * You'd only ever disable this in testing.
	 */
	var enabled = true
	
	@Pointcut("execution(* uk.ac.warwick.courses.commands.Command.apply(..)) && target(callee)")
	def applyCommand(callee:Describable[_]): Unit = {}
	
	@Around("applyCommand(callee)")
	def aroundApplyCommand[T](jp:ProceedingJoinPoint, callee:Describable[T]):Any = 
		if (enabled) {
			if (maintenanceCheck(callee)) recordEvent(callee) { jp.proceed.asInstanceOf[T] }
			else throw maintenanceMode.exception()
		} else {
			jp.proceed.asInstanceOf[T]
		}	
	
	def maintenanceCheck(callee:Describable[_]) = !maintenanceMode.enabled || callee.isInstanceOf[ReadOnly]
}
