package uk.ac.warwick.tabula.scheduling

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin.SchedulingSysadminController
import org.springframework.beans.factory.annotation.Autowired

class ApplicationTest extends AppContextTestBase {
    
    @Autowired var sysadminController: SchedulingSysadminController = _
       
    @Test def itWorks = {
    	assert(beans.containsBean("userLookup"))
    }
    
    // Can resolve message codes from any controller
    @Test def messageResolving = {
    	sysadminController.getMessage("NotEmpty") should be ("You need to put something here.")
    	sysadminController.getMessage("userId.notingroup", "alvin") should be ("The usercode alvin isn't in this group.")
    }

}
