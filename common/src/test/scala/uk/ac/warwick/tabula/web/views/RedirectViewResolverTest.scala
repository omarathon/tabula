package uk.ac.warwick.tabula.web.views

import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.support.SessionFlashMapManager
import org.springframework.web.servlet.view.RedirectView
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.ControllerViews
import uk.ac.warwick.tabula.{Fixtures, HttpMocking, TestBase}

class RedirectViewResolverTest extends TestBase with HttpMocking {

  val request: MockHttpServletRequest = mockRequest
  request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager)
  val response: MockHttpServletResponse = mockResponse
  val resolver = new RedirectViewResolver
  resolver.toplevelUrl = "https://tabula.warwick.ac.uk"


  def resolve(viewName: String): Option[String] = resolver.resolveViewName(viewName, null) match {
    case redirect: RedirectView =>
      redirect.render(null, request, response)
      Some(response.getRedirectedUrl)
    case _ => None
  }

  @Test def redirectPage(): Unit = {
    resolve("redirect:/sysadmin/departments") should be(Some("https://tabula.warwick.ac.uk/sysadmin/departments"))
    resolve("sysadmin/departments") should be(None)
  }

  @Test def context(): Unit = {
    new ControllerViews {

      def requestInfo = None

      val chemistry = Fixtures.department("ch")

      val viewName: String = Redirect(Routes.cm2.admin.department(chemistry)).viewName
      resolve(viewName) should be(Some("https://tabula.warwick.ac.uk/coursework/admin/department/ch"))
    }
  }

  @Test def redirectToRoot(): Unit = {
    resolve("redirect:/") should be(Some("https://tabula.warwick.ac.uk/"))
  }

}
