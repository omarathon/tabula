package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller

@Controller
class ErrorController {

  // TODO some extra error reporting
  
  @RequestMapping(Array("/error"))
  def generalError = {
    "errors/500"
  }
  
  @RequestMapping(Array("/error/404"))
  def pageNotFound = {
    "errors/404"
  }
  
}