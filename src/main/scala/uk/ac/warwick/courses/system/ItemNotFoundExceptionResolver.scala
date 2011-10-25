package uk.ac.warwick.courses.system

import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.HandlerExceptionResolver
import uk.ac.warwick.courses.web.controllers.Mav
import javax.servlet.http.HttpServletResponse

class ItemNotFoundExceptionResolver extends HandlerExceptionResolver {

  // Just returns the 404 page.
  def resolveException(
      request: HttpServletRequest, 
      response: HttpServletResponse, 
      o: Object, 
      exception: Exception) = Mav("errors/404")

}