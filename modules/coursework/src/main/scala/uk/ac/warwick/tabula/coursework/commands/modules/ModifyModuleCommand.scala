package uk.ac.warwick.tabula.coursework.commands.modules

import uk.ac.warwick.tabula.coursework
import uk.ac.warwick.tabula.coursework._

import commands.{ Description, Command }
import data.model.{ Department, Module }

abstract class ModifyModuleCommand extends Command[Module] {

	def department: Department

}
