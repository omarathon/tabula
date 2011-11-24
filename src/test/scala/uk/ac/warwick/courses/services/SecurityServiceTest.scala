package uk.ac.warwick.courses.services
import org.hibernate.annotations.AccessType
import org.junit.Test

import javax.persistence.Entity
import uk.ac.warwick.courses.actions.View
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.TestBase

class SecurityServiceTest extends TestBase {
	
	/*
	 * Just testing a compiler warning that scared me, about it
	 * mismatching case classes that use inheritence. But it's
	 * only when the superclass is a case class, which we don't do. 
	 * I'll leave this in though!
	 */
	@Test def caseMatching {
	  val action = View(new Department)
	  
	  (action:Any) match {
	    case View(d:Department) => {}
	    case _ => fail("Should have matched view")
	  }
	}
	
}