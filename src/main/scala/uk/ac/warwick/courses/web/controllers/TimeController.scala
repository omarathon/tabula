package uk.ac.warwick.courses.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.beans.factory.annotation.Value
import java.io.Writer
import java.util.Properties
import java.util.Date
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.web.Mav

@Controller
class TimeController {

  @BeanProperty var timeWelcome = "Hello"

  @RequestMapping(Array("/time"))
  def showTime = Mav("time/view", 
    "time" -> new Date,
    "timeWelcome" -> timeWelcome
    )
    
  @RequestMapping(Array("/time/null"))
  def error = throw new NullPointerException

}