package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import scala.actors.Actor
import scala.actors.Actor._

class ScheduledJobsTest extends TestBase {

	/** Just a thread that will stop until we call go() on it,
	 * and will call nonconcurrent to check that an instance of the
	 * task isn't already running. */
	case class ControllableTask(sync:SchedulingConcurrency) extends Thread {
		private val lock = new Object
		private var paused = true
		var running = false
		override def run { 
			sync.nonconcurrent("a") {
				running = true
				while (paused) lock.synchronized { lock.wait }
			}
		}
		def go {
			paused = false
			lock.synchronized { lock.notify	}
		}
	}

	/**
	 * Test that when one thread is running in a nonconcurrent block,
	 * a second thread will skip that block.
	 */
	@Test(timeout=5000) def schedulingConcurrency {
		val s = new Object with SchedulingConcurrency
		val thread = new ControllableTask(s)
		val thread2 = new ControllableTask(s)
		
		// start a paused thread and wait til it's properly running
		thread.start
		while (!thread.running) {}
		
		// run thread2 synchronously - it should return straight away
		// because thread 1 is holding the metaphorical flag.
		thread2.run
		
		// let thread 1 finish. 
		thread.go
		thread.join
	}
	
}