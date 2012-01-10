package uk.ac.warwick.courses.commands.modules

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Module
import scala.reflect.BeanProperty
import collection.JavaConversions._

class AddPermissionCommand extends Command[Unit] {

	@BeanProperty var module:Module =_
	@BeanProperty var usercodes:JList[String] =_
	
	def apply {
		
	}
	
	def describe(d:Description) = d.module(module).properties(
			"userids" -> usercodes.mkString(",")
	)
	
}