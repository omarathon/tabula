package uk.ac.warwick.tabula.web.controllers.common

import java.io._

import com.itextpdf.text.ExceptionConverter
import javax.servlet.http.HttpServletResponse
import org.springframework.beans.BeanWrapperImpl
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.NullCommand
import uk.ac.warwick.tabula.helpers.Logging

@Controller
@RequestMapping(Array("/test"))
class DiagnosticController extends Logging {
	val startTime: Long = System.currentTimeMillis
	val features: Features = Wire[Features]

	@RequestMapping(Array("/up")) def test(out: Writer): Unit =
		out.write(String.valueOf(((System.currentTimeMillis - startTime) * 0.001).toLong))

	@RequestMapping(Array("/feature/{name}")) def feature(@PathVariable name: String, out: Writer): Unit = {
		val values = new BeanWrapperImpl(features)
		out.write(values.getPropertyValue(name).asInstanceOf[Boolean].toString)
	}

	@RequestMapping(Array("/error")) def error = throw new RuntimeException("Deliberately generated exception")

	@RequestMapping(Array("/error-command")) def errCommand: Unit = {
		val command = new NullCommand
		logger.info("About to throw an exception")
		command will { () =>
			logger.info("Yep, definitely throwing...")
			throw new RuntimeException("Deliberately generated exception")
		}
		command.apply()
	}

	@RequestMapping(Array("/error/client-abort")) def clientAbortError(response: HttpServletResponse): Nothing = {
		response.getOutputStream.print("Partial content downlo")
		throw Class.forName("org.apache.catalina.connector.ClientAbortException").newInstance().asInstanceOf[IOException]
	}

	@RequestMapping(Array("/error/pdf-client-abort")) def iTextClientAbortError(response: HttpServletResponse): Nothing = {
		response.getOutputStream.print("Partial content downlo")
		val e = Class.forName("org.apache.catalina.connector.ClientAbortException").newInstance().asInstanceOf[IOException]
		throw new ExceptionConverter(e)
	}

	@RequestMapping(method = Array(RequestMethod.POST), value = Array("/upload"))
	def testUpload(@RequestParam("file") file: MultipartFile, out: Writer): Unit = {
		logger.info(s"Uploaded: ${file.getOriginalFilename} - ${file.getSize}")
		out.write(s"Uploaded: ${file.getOriginalFilename} - ${file.getSize}")
	}
}