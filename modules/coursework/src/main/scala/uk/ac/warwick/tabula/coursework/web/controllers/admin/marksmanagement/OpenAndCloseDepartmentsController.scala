package uk.ac.warwick.tabula.coursework.web.controllers.admin.marksmanagement

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.commands.marksmanagement.OpenAndCloseDepartmentsCommand
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.data.model.DegreeType

@Controller
@RequestMapping(value=Array("/admin/marksmanagement/departments"))
class OpenAndCloseDepartmentsController extends BaseController {

	type OpenAndCloseDepartmentsCommand = Appliable[DegreeType] with PopulateOnForm

	@ModelAttribute("command")
	def command: OpenAndCloseDepartmentsCommand = OpenAndCloseDepartmentsCommand()

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("command") cmd: OpenAndCloseDepartmentsCommand):Mav = {
		cmd.populate()
		Mav("admin/marksmanagement/open_close_departments")
	}

	@RequestMapping(method=Array(POST))
	def submit(@ModelAttribute("command") cmd: OpenAndCloseDepartmentsCommand): Mav = {
		val degreeTypeUpdated: DegreeType = cmd.apply()
		val mav = Mav("admin/marksmanagement/open_close_departments")
		if (degreeTypeUpdated == DegreeType.Undergraduate) {
			mav.addObjects("undergraduateUpdated" -> true)
		}	else {
			mav.addObjects("postgraduateUpdated" -> true)
		}
		mav
	}
}