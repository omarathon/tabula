package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/sysadmin/mappings"))
class RequestMappingsController {

	lazy val handlerMapping: RequestMappingHandlerMapping = Wire[RequestMappingHandlerMapping]

	case class Mapping(
		pattern: String,
		methods: String,
		params: String,
		headers: String,
		consumes: String,
		produces: String
	)

	@RequestMapping
	def listMappings(): Mav = {
		def mappings(info: RequestMappingInfo, method: HandlerMethod): Iterable[Mapping] = {
			val methods = info.getMethodsCondition.toString
			val params = info.getParamsCondition.toString
			val headers = info.getHeadersCondition.toString
			val consumes = info.getConsumesCondition.toString
			val produces = info.getProducesCondition.toString

			info.getPatternsCondition.getPatterns.asScala.map { pattern =>
				Mapping(pattern, methods, params, headers, consumes, produces)
			}
		}

		Mav("sysadmin/mappings",
			"mappings" -> handlerMapping.getHandlerMethods.asScala.toSeq.flatMap { case (info, method) => mappings(info, method) }.sortBy(_.pattern)
		)
	}

}
