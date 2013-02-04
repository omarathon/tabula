package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.springframework.web.servlet.view.RedirectView
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.HttpMocking
import collection.JavaConversions.asJavaMap
import collection.mutable
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.support.SessionFlashMapManager

class RedirectViewResolverTest extends TestBase with HttpMocking {

  @Test def redirectPage {
    val request = mockRequest
    request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager);
    
    val response = mockResponse
	val resolver = new RedirectViewResolver
	resolver.setToplevelUrl("https://tabula.warwick.ac.uk")
	resolver.setContext("/")
	
	resolver.resolveViewName("redirect:/sysadmin/departments", null) match {
	  case redirect:RedirectView => {
	    redirect.render(null, request, response)
	    response.getRedirectedUrl() should be ("https://tabula.warwick.ac.uk/sysadmin/departments")
	  }
	}
    
    resolver.resolveViewName("sysadmin/departments", null) should be (null)
  }
  
}