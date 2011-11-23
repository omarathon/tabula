package uk.ac.warwick.courses.web.controllers

import java.util.Calendar
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.beans.factory.annotation.Value
import java.io.Writer
import java.util.Properties
import java.util.Date

@Controller
class TimeController {

  var timeWelcome = "Hello"

  @RequestMapping(value = Array("/time"))
  def showTime = new ModelAndView("time/view") {
    addObject("time", new Date)
    addObject("timeWelcome", timeWelcome)
  }

  def setTimeWelcome(x: String) = {
    timeWelcome = x
  }

}