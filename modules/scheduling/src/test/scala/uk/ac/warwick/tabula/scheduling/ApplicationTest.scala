package uk.ac.warwick.tabula.scheduling

import org.junit.Test
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin.SysadminController
import org.junit.Test
import uk.ac.warwick.tabula.services.UserLookupService


class ApplicationTest extends AppContextTestBase {
    
    lazy val sysadminController = Wire[SysadminController]
       
    @Test def itWorks = {
    	assert(Wire[UserLookupService] != null)
    }
    
    // Can resolve message codes from any controller
    @Test def messageResolving = {
    	sysadminController.getMessage("NotEmpty") should be ("You need to put something here.")
    	sysadminController.getMessage("userId.notingroup", "alvin") should be ("The usercode alvin isn't in this group.")
    }

}
