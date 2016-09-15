package uk.ac.warwick.tabula.web.views

import org.springframework.test.context.TestPropertySource
import uk.ac.warwick.tabula.{AppContextTestBase, HttpMocking}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.support.SessionFlashMapManager
import uk.ac.warwick.tabula.web.controllers.ControllerViews
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.data.model.Department

@TestPropertySource(properties = Array("cm1.prefix = coursework"))
class RedirectViewResolverTest extends AppContextTestBase with HttpMocking {

	val request = mockRequest
	request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager)
	val response = mockResponse
	val resolver = new RedirectViewResolver
	resolver.toplevelUrl = "https://tabula.warwick.ac.uk"


	def resolve(viewName: String) = resolver.resolveViewName(viewName, null) match {
		case redirect:RedirectView =>
			redirect.render(null, request, response)
			Some(response.getRedirectedUrl)
		case _ => None
	}

	@Test def redirectPage() {
		resolve("redirect:/sysadmin/departments") should be (Some("https://tabula.warwick.ac.uk/sysadmin/departments"))
		resolve("sysadmin/departments") should be (None)
	}

	@Test def context() {
		new ControllerViews {
			def requestInfo = None
			val chemistry = new Department {
				code = "ch"
			}

			val viewName = Redirect(Routes.coursework.admin.department(chemistry)).viewName
			resolve(viewName) should be (Some("https://tabula.warwick.ac.uk/coursework/admin/department/ch/"))
		}
	}

	@Test def redirectToRoot() {
		resolve("redirect:/") should be (Some("https://tabula.warwick.ac.uk/"))
	}

}