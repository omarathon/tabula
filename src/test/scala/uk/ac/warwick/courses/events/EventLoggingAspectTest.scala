package uk.ac.warwick.courses.events
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.Aspects
import org.junit.Test
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.Mockito
import collection.JavaConversions._
import uk.ac.warwick.courses.commands._
import org.mockito.Matchers.{eq => isEq}

class EventLoggingAspectTest extends TestBase with Mockito {
	
	
	val aspect = Aspects.aspectOf(classOf[EventLoggingAspect])
	
	@Test def aspectsApplied {
		val command = new NullCommand().will { () => 
			println("Ain't doing nothing today") 
		}
			
		val listener = mock[EventListener]
		aspect.listener = listener
		
		command()

		there was one(listener).beforeCommand(any[Event])
		there was one(listener).afterCommand(any[Event], isEq(null))
	}
	
	@Test def exceptionHandlerCalled {
		val command = new NullCommand().will { () => 
			throw new NullPointerException
		}
		
		val listener = mock[EventListener]
		aspect.listener = listener
		
		try {
			command()
			fail("didn't throw an exception")
		} catch {
			case npe:NullPointerException =>
				there was one(listener).onException(any[Event], isEq(npe))
		}
		
		there was one(listener).beforeCommand(any[Event])
	}
	
}