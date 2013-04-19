package uk.ac.warwick.tabula.profiles

import org.junit.Test
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._

class ApplicationTest extends AppContextTestBase {
    
    lazy val annotationMapper = Wire[RequestMappingHandlerMapping]
       
    @Test def itWorks = {
    	assert(Wire.named("userLookup") != null)
    }

}
