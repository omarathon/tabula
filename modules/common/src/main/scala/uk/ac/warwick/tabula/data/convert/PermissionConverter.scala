package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter

class PermissionConverter extends TwoWayConverter[String, Permission] {
	
	override def convertLeft(permission: Permission) = (Option(permission) map { _.getName }).orNull
  
	override def convertRight(name: String) = 
		if (!name.hasText) null
		else try { Permissions.of(name) } catch { case e: IllegalArgumentException => null }

}