package uk.ac.warwick.courses

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.mock.web.MockServletContext

class ApplicationTest extends JUnitSuite with ShouldMatchersForJUnit {

	@Test def applicationContextInitialises:Unit = {
	  val context = new AnnotationConfigWebApplicationContext
	  context.setConfigLocation(classOf[uk.ac.warwick.courses.config.Application].getName)
	  context.setServletContext(new MockServletContext)
	  context.refresh()
	  context.getBean("timeController")
	}

}