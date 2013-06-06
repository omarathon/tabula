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

	trait Context {
		val request = mockRequest
		request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager);
		val response = mockResponse
		val resolver = new RedirectViewResolver
		resolver.toplevelUrl = "https://tabula.warwick.ac.uk"
		resolver.context = "/"

		def resolve(viewName: String) = resolver.resolveViewName(viewName, null) match {
			case redirect:RedirectView => {
				redirect.render(null, request, response)
				Some(response.getRedirectedUrl())
			}
			case _ => None
		}
	}

  @Test def redirectPage {
		new Context {
			resolve("redirect:/sysadmin/departments") should be (Some("https://tabula.warwick.ac.uk/sysadmin/departments"))
			resolve("sysadmin/departments") should be (None)
		}
	}

	@Test def context {
		new Context {
			resolver.context = "/coursework"
			resolve("redirect:/admin") should be (Some("https://tabula.warwick.ac.uk/coursework/admin"))
		}
	}

	@Test def redirectToRoot {
		new Context {
			resolve("redirect:/") should be (Some("https://tabula.warwick.ac.uk/"))
		}
		new Context {
			resolver.context = "/coursework"
			resolve("redirect:/") should be (Some("https://tabula.warwick.ac.uk/coursework/"))
		}
	}

}