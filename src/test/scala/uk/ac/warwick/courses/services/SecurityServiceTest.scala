package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.data.model.Department

class SecurityServiceTest extends TestBase {
	@Test def caseMatching {
	  val action = View(new Department)
	  
	  (action:Any) match {
	    case View(d:Department) => {}
	    case _ => fail("Should have matched view")
	  }
	}
}