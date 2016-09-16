package uk.ac.warwick.tabula.web.controllers.coursework.admin.marksmanagement

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.marksmanagement.OpenAndCloseDepartmentsCommand
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.DegreeType

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/coursework/admin/marksmanagement/departments"))
class OldOpenAndCloseDepartmentsController extends BaseController {

	type OpenAndCloseDepartmentsCommand = Appliable[DegreeType] with PopulateOnForm

	@ModelAttribute("command")
	def command: OpenAndCloseDepartmentsCommand = OpenAndCloseDepartmentsCommand()

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("command") cmd: OpenAndCloseDepartmentsCommand):Mav = {
		cmd.populate()
		Mav("coursework/admin/marksmanagement/open_close_departments")
	}

	@RequestMapping(method=Array(POST))
	def submit(@ModelAttribute("command") cmd: OpenAndCloseDepartmentsCommand): Mav = {
		val degreeTypeUpdated: DegreeType = cmd.apply()
		val mav = Mav("coursework/admin/marksmanagement/open_close_departments")
		if (degreeTypeUpdated == DegreeType.Undergraduate) {
			mav.addObjects("undergraduateUpdated" -> true)
		}	else {
			mav.addObjects("postgraduateUpdated" -> true)
		}
		mav
	}
}