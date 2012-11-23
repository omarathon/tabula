package uk.ac.warwick.tabula.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import java.io.Writer
import uk.ac.warwick.tabula.commands.NullCommand
import uk.ac.warwick.tabula.helpers.Logging

@Controller
@RequestMapping(Array("/test"))
class DiagnosticController extends Logging {
	val startTime = System.currentTimeMillis

	@RequestMapping(Array("/up")) def test(out: Writer) =
		out.write(String.valueOf(((System.currentTimeMillis - startTime) * 0.001).toLong))

	@RequestMapping(Array("/error")) def error = throw new RuntimeException("Deliberately generated exception")

	@RequestMapping(Array("/error-command")) def errCommand = {
		val command = new NullCommand
		logger.info("About to throw an exception")
		command will {
			logger.info("Yep, definitely throwing...")
			throw new RuntimeException("Deliberately generated exception")
		}
		command.apply()
	}
}