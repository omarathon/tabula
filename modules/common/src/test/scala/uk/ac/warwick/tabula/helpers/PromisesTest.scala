package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.concurrency.promise.UnfulfilledPromiseException

class PromisesTest extends TestBase with Promises {
	
	@Test def mutable {
		val p = promise[String]
		
		try {
			p.fulfilPromise()
			fail("expected exception")
		} catch { case e: UnfulfilledPromiseException => }
		
		p.setValue("steve")
		
		p.fulfilPromise() should be ("steve")
	}
	
	@Test def functional {
		var timesRun = 0
		val p = promise {
			timesRun += 1
			"steve"
		}
		
		timesRun should be (0)
		p.fulfilPromise() should be ("steve")
		timesRun should be (1)
		p.fulfilPromise() should be ("steve")
		timesRun should be (1)
	}
	
	@Test def optional {
		var timesRun = 0
		val p1 = optionPromise {
			timesRun += 1
			Some("steve")
		}
		
		timesRun should be (0)
		p1.fulfilPromise() should be ("steve")
		timesRun should be (1)
		p1.fulfilPromise() should be ("steve")
		timesRun should be (1)
		
		timesRun = 0
		val p2 = optionPromise[String] {
			timesRun += 1
			None
		}
		
		timesRun should be (0)
		
		try {
			p2.fulfilPromise()
			fail("expected exception")
		} catch { case e: UnfulfilledPromiseException => }
		timesRun should be (1)
		
		try {
			p2.fulfilPromise()
			fail("expected exception")
		} catch { case e: UnfulfilledPromiseException => }
		timesRun should be (2) // we keep running until it returns the right value!
	}

}