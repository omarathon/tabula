package uk.ac.warwick.tabula.web.controllers.ajax

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/ajax/smallgrouppicker/query"))
class SmallGroupPickerController extends BaseController {

	@ModelAttribute("command")
	def command = SmallGroupPickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command") cmd: Appliable[Seq[SmallGroup]]): Mav = {
		val results = cmd.apply()
		Mav(
			new JSONView(
				results.map(result => Map(
					"id" -> result.id,
					"name" -> result.name,
					"groupSet" -> Map(
						"id" -> result.groupSet.id,
						"name" -> result.groupSet.name
					),
					"module" -> Map(
						"code" -> result.groupSet.module.code
					)
				))
			)
		)
	}

}

class SmallGroupPickerCommand extends CommandInternal[Seq[SmallGroup]] {

	self: SmallGroupServiceComponent =>

	var query: String = _

	def applyInternal(): Seq[SmallGroup] = {
		if (!query.hasText) {
			Seq()
		} else {
			smallGroupService.findSmallGroupsByNameOrModule(query)
		}
	}

}

object SmallGroupPickerCommand {
	def apply() = new SmallGroupPickerCommand with Command[Seq[SmallGroup]]
	with AutowiringSmallGroupServiceComponent
	with ReadOnly with Unaudited with Public
}
