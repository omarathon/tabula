package uk.ac.warwick.tabula.coursework.web.controllers.sysadmin

import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.Features
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.beans.BeanWrapperImpl
import collection.JavaConversions._
import java.beans.PropertyDescriptor
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.coursework.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import scala.annotation.target.field
import scala.annotation.target.param
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.BeanWrapper

case class FeatureItem(val name: String, val value: Boolean)

/**
 * Read and write feature flags. Alternative to using JMX.
 *
 * TODO unfinished
 */
@Controller
@RequestMapping(value = Array("/sysadmin/features"))
final class FeaturesController extends BaseController with InitializingBean {

	@Autowired var features: Features = _

	private var wrapper: BeanWrapper = _
	private var properties: List[PropertyDescriptor] = _

	override def afterPropertiesSet {
		wrapper = new BeanWrapperImpl(features)
		properties = wrapper.getPropertyDescriptors.toList
			.filter { _.getPropertyType.isAssignableFrom(classOf[java.lang.Boolean]) }
			.sortBy { _.getDisplayName }
	}

	def currentValues = properties.map { (property) =>
		new FeatureItem(property.getDisplayName, wrapper.getPropertyValue(property.getName).asInstanceOf[java.lang.Boolean])
	}

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(): Mav = {
		Mav("sysadmin/features",
			"currentValues" -> currentValues)
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def update(@RequestParam("name") name: String, @RequestParam("value") value: Boolean): Mav = {
		properties.find { _.getName == name } match {
			case Some(property) => {
				wrapper.setPropertyValue(property.getName, value)
			}
			case None => throw new IllegalArgumentException
		}
		Redirect("/sysadmin/features")
	}
}