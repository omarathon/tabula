package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.util.core.StringUtils

class PermissionConverter extends Converter[String, Permission] {
  
	override def convert(name: String) = 
		if (!StringUtils.hasText(name)) null
		else Permissions.of(name)

}