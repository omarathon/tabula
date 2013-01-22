package uk.ac.warwick.tabula.coursework.commands.modules

import uk.ac.warwick.tabula
import uk.ac.warwick.tabula._
import commands.{ Description, Command }
import data.model.{ Department, Module }
import uk.ac.warwick.tabula.actions.Sysadmin

abstract class ModifyModuleCommand extends Command[Module] {
	
	PermissionsCheck(Sysadmin())

	def department: Department

}
