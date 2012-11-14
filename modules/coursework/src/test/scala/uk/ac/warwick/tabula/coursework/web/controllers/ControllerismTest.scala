package uk.ac.warwick.tabula.coursework.web.controllers

import org.junit.Test
import uk.ac.warwick.tabula.coursework.TestBase


import org.springframework.validation.Errors
import org.springframework.validation.BindException

class ControllerismTest extends TestBase {
	@Test def validatesWith {
		val controller = new BaseController {
			validatesWith[String] { (cmd:String, errors:Errors) =>
				if (cmd != "hello") errors.reject("nope")
			}
		}
		
		val errors = new BindException("hello", "command")
		controller.validator.validate("hello", errors)
		errors.hasErrors should be (false)
		
		val errors2 = new BindException("hello", "command")
		controller.validator.validate("hebbo", errors2)
		errors2.hasErrors should be (true)
	}
}