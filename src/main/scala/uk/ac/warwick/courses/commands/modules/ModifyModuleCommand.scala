package uk.ac.warwick.courses.commands.modules

import uk.ac.warwick.courses
import uk.ac.warwick.courses._

import commands.{Description, Command}
import data.model.{Department, Module}

abstract class ModifyModuleCommand extends Command[Module] {

	def department: Department

}
