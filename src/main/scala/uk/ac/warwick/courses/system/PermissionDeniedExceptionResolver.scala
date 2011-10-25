package uk.ac.warwick.courses.system

import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.courses.web.controllers.Controllerism
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.web.controllers.Mav

class PermissionDeniedExceptionResolver extends HandlerExceptionResolver {

  def resolveException(
      request: HttpServletRequest, 
      response: HttpServletResponse, 
      o: Object, 
      exception: Exception) = Mav("errors/permissionDenied")

}