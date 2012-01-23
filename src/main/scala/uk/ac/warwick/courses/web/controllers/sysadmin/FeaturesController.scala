package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.Features
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.beans.BeanWrapperImpl
import collection.JavaConversions._
import java.beans.PropertyDescriptor
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import scala.annotation.target.field
import scala.annotation.target.param

case class FeatureItem(val name:String, val value:Boolean)

/**
 * Read and write feature flags. Alternative to using JMX.
 * 
 * TODO unfinished
 */
@Controller
@RequestMapping(value=Array("/sysadmin/features"))
final class FeaturesController @Autowired()(val features:Features) extends BaseController {
	val wrapper = new BeanWrapperImpl(features)
	private val properties = wrapper.getPropertyDescriptors.toList
									.filter{ _.getPropertyType.isAssignableFrom(classOf[java.lang.Boolean]) }
									.sortBy{ _.getDisplayName }
									
	def currentValues = properties.map { (property) =>
		new FeatureItem(property.getDisplayName, wrapper.getPropertyValue(property.getName).asInstanceOf[java.lang.Boolean] )
	}
									
	@RequestMapping(method=Array(RequestMethod.GET))
	def get(): Mav = {
		Mav("sysadmin/features",
				"currentValues" -> currentValues)
	}
	
	@RequestMapping(method=Array(RequestMethod.POST))
	def update(@RequestParam name:String, @RequestParam value:Boolean): Mav = {
		properties.find { _.getName == name } match {
			case Some(property) => property.getWriteMethod.invoke(features, Array(value))
			case None => throw new IllegalArgumentException
		}
		Redirect("/sysadmin/features")
	}
}