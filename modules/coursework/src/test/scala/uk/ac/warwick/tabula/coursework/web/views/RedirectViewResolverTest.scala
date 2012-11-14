package uk.ac.warwick.tabula.coursework.web.views

import uk.ac.warwick.tabula.coursework.TestBase
import org.junit.Test
import org.springframework.web.servlet.view.RedirectView
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.coursework.HttpMocking

import collection.JavaConversions.asJavaMap
import collection.mutable

class RedirectViewResolverTest extends TestBase with HttpMocking {

  @Test def redirectPage {
    val request = mockRequest
    val response = mockResponse
	val resolver = new RedirectViewResolver
	resolver.setToplevelUrl("https://coursework.warwick.ac.uk")
	
	resolver.resolveViewName("redirect:/sysadmin/departments", null) match {
	  case redirect:RedirectView => {
	    redirect.render(null, request, response)
	    response.getRedirectedUrl() should be ("https://coursework.warwick.ac.uk/sysadmin/departments")
	  }
	}
    
    resolver.resolveViewName("sysadmin/departments", null) should be (null)
  }
  
}