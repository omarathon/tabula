package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.data.Transactions
import uk.ac.warwick.courses._

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

  @Test def canDo {
  	Transactions.disable {
		val service = new SecurityService
		val department = new Department
		department.addOwner("cusfal")
		withUser("cusebr") {
			val info = RequestInfo.fromThread.get
			val user = info.user
			service.can(user, Manage(department)) should be (false)
		}
	}
  }
	
	@Test def factory {
		val dept = new Department
		val viewIt = Action.of("View", dept)
		
		viewIt.getClass should be (classOf[View])
	}
	
}