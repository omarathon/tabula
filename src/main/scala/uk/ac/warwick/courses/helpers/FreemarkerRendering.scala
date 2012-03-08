package uk.ac.warwick.courses.helpers

import java.io.StringWriter
import freemarker.template.Template
import freemarker.template.Configuration

/**
 * Methods for out-of-request template rendering.
 * 
 * TODO rely a lot on JSP taglib support which currently only works in a request,
 * 		so we can't use JSP tags here.
 * 		Work out some way of attaching pretend context to make it work?
 */
trait FreemarkerRendering {
	//def configuration:Configuration
	
	def renderToString(template:Template, model:Any): String = {
		val writer = new StringWriter
		template.process(model, writer)
		writer.toString
	}
	
	def renderToString(template:String, model:Any)(implicit config:Configuration): String = {
		renderToString(config.getTemplate(template), model)
	}
}