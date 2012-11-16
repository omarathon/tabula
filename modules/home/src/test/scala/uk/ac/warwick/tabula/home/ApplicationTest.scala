package uk.ac.warwick.tabula.home

import org.junit.Test
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula._

class ApplicationTest extends AppContextTestBase {
    
    @Autowired var annotationMapper:RequestMappingHandlerMapping =_
       
    @Test def itWorks = {
    	assert(beans.containsBean("userLookup"))
    }

}
