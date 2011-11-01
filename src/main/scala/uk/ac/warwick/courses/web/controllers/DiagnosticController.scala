package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import java.io.Writer

@Controller
@RequestMapping(Array("/test"))
class DiagnosticController {
	val startTime = System.currentTimeMillis
	
	@RequestMapping(Array("/up")) def test(out:Writer) =
		out.write(String.valueOf(((System.currentTimeMillis - startTime)*0.001).toLong))
	
	
}