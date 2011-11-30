package uk.ac.warwick.courses.web.controllers

import org.junit.Test
import uk.ac.warwick.courses.TestBase
import org.springframework.validation.Errors
import org.springframework.validation.BindException

class ControllerismTest extends TestBase {
	@Test def validatesWith {
		val controller = new Controllerism {
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