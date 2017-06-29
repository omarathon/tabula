package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

/**
	* Created by ritchie on 28/06/17.
	*/
class TestyMcTestFace extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to set up some assignments" in {
		withAssignment("xxx01", "Fully featured assignment") { id =>

			Given("I can see my new assignment is there")
			id should not be ('empty)

		}
	}

}
