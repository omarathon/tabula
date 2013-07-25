package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.helpers.StringUtils._

class CreateSmallGroupSetCommand(module: Module) extends ModifySmallGroupSetCommand(module) with BindListener {
	
	PermissionCheck(Permissions.SmallGroups.Create, module)

	val setOption = None
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		// We set the promised value here so that sub-commands work
		val set = { promisedValue = new SmallGroupSet(module) }
		copyTo(set)
		service.saveOrUpdate(set)
		set
	}
	
	override def onBind(result: BindingResult) {
		super.onBind(result)
		
		// If we haven't set a name, make one up
		if (!name.hasText) {
			Option(format) foreach { format => name = "%s %ss".format(module.code.toUpperCase, format) }
		}
	}

	override def describeResult(d: Description, smallGroupSet: SmallGroupSet) = d.smallGroupSet(smallGroupSet)

	override def describe(d: Description) = d.module(module).properties(
		"name" -> name,
		"format" -> format)
	
}