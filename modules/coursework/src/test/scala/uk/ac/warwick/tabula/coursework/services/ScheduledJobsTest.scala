package uk.ac.warwick.tabula.coursework.services

import uk.ac.warwick.tabula.coursework.TestBase
import SchedulingConcurrency._

import org.junit.Test
import scala.actors.Actor
import scala.actors.Actor._

class ScheduledJobsTest extends TestBase {

	/** Just a thread that will stop until we call go() on it,
	 * and will call nonconcurrent to check that an instance of the
	 * task isn't already running. */
	class ControllableTask extends Thread {
		private val lock = new Object
		private val startLock = new Object
		private var paused = true
		var running = false
		override def run { 
			nonconcurrent("a") {
				running = true
				startLock.synchronized { startLock.notifyAll }
				try while (paused) lock.synchronized { lock.wait }
				finally running = false
			}
		}
		def go {
			paused = false
			lock.synchronized { lock.notifyAll }
		}
		def waitToStart {
			startLock.synchronized {
				while (!running) startLock.wait
			}
		}
	}

	/**
	 * Test that when one thread is running in a nonconcurrent block,
	 * a second thread will skip that block.
	 */
	@Test(timeout=5000) def schedulingConcurrency = for (i <- 1 to 50) {
		val thread = new ControllableTask
		val thread2 = new ControllableTask
		
		// start a paused thread and wait til it's properly running
		thread.start
		thread.waitToStart
		
		// run thread2 synchronously - it should return straight away
		// because thread 1 is holding the metaphorical flag.
		thread2.run
		
		// let thread 1 finish. 
		thread.go
		thread.join
		
	}
	
}