package uk.ac.warwick.tabula.web

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.web.controllers.sysadmin.SysadminController

class ApplicationTest extends AppContextTestBase {

	@Autowired var sysadminController: SysadminController = _

	@Test def itWorks(): Unit = {
		assert(beans.containsBean("userLookup"))
	}

	// Can resolve message codes from any controller
	@Test def messageResolving(): Unit = {
		sysadminController.getMessage("NotEmpty") should be("A required field was empty.")
		sysadminController.getMessage("userId.notingroup", "alvin") should be("The usercode alvin isn't in this group.")
	}

}
