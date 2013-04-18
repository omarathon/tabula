package uk.ac.warwick.tabula

import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{expect => expecting}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services._
import org.junit.Before

trait LenientUserLookup {

	var userLookup = Wire[SwappableUserLookupService]
	
	@Before def setup {
		userLookup.delegate = lenientUserLookup
	}
	
	def lenientUserLookup = {
	    val backend = mock[UserLookupService]
		expecting {
			val id = capturingParam[String]
		    allowing(backend).getUserByWarwickUniId(id.capture) willReturn id.map{
				new User(_) {
					setFoundUser(true)
				}
			}
		}
	    backend
    }
}