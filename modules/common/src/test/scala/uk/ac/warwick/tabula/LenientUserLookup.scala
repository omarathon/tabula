package uk.ac.warwick.tabula

import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{expect => expecting}
import uk.ac.warwick.userlookup.User
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services._
import org.junit.Before

trait LenientUserLookup {

	@Autowired var userLookup:SwappableUserLookupService =_
	
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