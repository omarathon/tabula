package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.util.core.ExceptionUtils
import javax.servlet.ServletException
import org.springframework.web.bind.annotation.RequestHeader

@Controller
class ErrorController {

  // TODO some extra error reporting
   
  @RequestMapping(Array("/error"))
  def generalError(mav:ModelAndView, request:HttpServletRequest) = {
    request.getAttribute("javax.servlet.error.exception") match {
      case exception:Exception => {
        mav.addObject("originalException")
        mav.addObject("exception", ExceptionUtils.getInterestingThrowable(exception, Array( classOf[ServletException] )))
      }
    }
    "errors/500"
  }
  
  @RequestMapping(Array("/error/404"))
  def pageNotFound(@RequestHeader("X-Requested-Uri") requestedUri:String) = {
    new ModelAndView("errors/404")
    	.addObject("requestedUri", requestedUri)
  }
  
}