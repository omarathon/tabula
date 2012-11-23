package uk.ac.warwick.tabula.actions

import org.junit.Test

import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.TestBase

class ActionsTest extends TestBase {
	
	@Test def of {
		val module = new Module()
		module.code = "cs101"
		Action.of("View", module) match {
			case View(module:Module) => module.code should be ("cs101")
			case what:Any => fail("what is this?" + what) 
		}
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def invalidAction {
		Action.of("Spank", new Module())
	}
}