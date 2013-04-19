package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.Features

import org.springframework.stereotype.Controller
import org.springframework.beans.BeanWrapperImpl
import collection.JavaConversions._
import java.beans.PropertyDescriptor
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import scala.annotation.target.field
import scala.annotation.target.param
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.FeaturesMessage
import uk.ac.warwick.tabula.JavaImports._

case class FeatureItem(val name: String, val value: Boolean)

/**
 * Read and write feature flags. Alternative to using JMX.
 */
@Controller
@RequestMapping(value = Array("/sysadmin/features"))
final class FeaturesController extends BaseController with InitializingBean {

	var features = Wire[Features]
	var queue = Wire[Queue]("settingsSyncTopic")

	private var wrapper: BeanWrapper = _
	private var properties: List[PropertyDescriptor] = _

	override def afterPropertiesSet {
		wrapper = new BeanWrapperImpl(features)
		properties = new BeanWrapperImpl(new FeaturesMessage).getPropertyDescriptors.toList
			.filter { _.getWriteMethod != null }
			.sortBy { _.getDisplayName }
	}

	def currentValues = properties.map { (property) =>
		new FeatureItem(property.getDisplayName, wrapper.getPropertyValue(property.getName).asInstanceOf[JBoolean])
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
		
		// Broadcast it to the world!
		queue.send(new FeaturesMessage(features))
		
		Redirect("/sysadmin/features")
	}
}