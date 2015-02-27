package uk.ac.warwick.tabula.web.controllers.common

import java.io.{IOException, Writer}
import javax.servlet.http.HttpServletResponse

import org.springframework.beans.BeanWrapperImpl
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.NullCommand
import uk.ac.warwick.tabula.helpers.Logging

@Controller
@RequestMapping(Array("/test"))
class DiagnosticController extends Logging {
	val startTime = System.currentTimeMillis
	val features = Wire[Features]

	@RequestMapping(Array("/up")) def test(out: Writer) =
		out.write(String.valueOf(((System.currentTimeMillis - startTime) * 0.001).toLong))
		
	@RequestMapping(Array("/feature/{name}")) def feature(@PathVariable("name") name: String, out: Writer) = {
		val values = new BeanWrapperImpl(features)
		out.write(values.getPropertyValue(name).asInstanceOf[Boolean].toString)
	}

	@RequestMapping(Array("/error")) def error = throw new RuntimeException("Deliberately generated exception")

	@RequestMapping(Array("/error-command")) def errCommand = {
		val command = new NullCommand
		logger.info("About to throw an exception")
		command will { () =>
			logger.info("Yep, definitely throwing...")
			throw new RuntimeException("Deliberately generated exception")
		}
		command.apply()
	}

	@RequestMapping(Array("/error/client-abort")) def clientAbortError(response: HttpServletResponse) = {
		response.getOutputStream.print("fuuuuuuuuuuuuuuuuu")
		throw Class.forName("org.apache.catalina.connector.ClientAbortException").newInstance().asInstanceOf[IOException]
	}
}