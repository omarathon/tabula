package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.util.core.StringUtils
import org.springframework.format.Formatter
import java.util.Locale

class ClassConverter extends TwoWayConverter[String, Class[_]] {
  	
	override def convertRight(className: String) = 
		if (StringUtils.hasText(className)) Class.forName(className)
		else null
		
	override def convertLeft(clazz: Class[_]) = Option(clazz) match {
		case Some(clazz) => clazz.getName
		case None => null
	}

}