package uk.ac.warwick.tabula.coursework.web.controllers.admin.markschemes

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.MarkScheme
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkSchemeDao
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes"))
class ListMarkSchemesController extends CourseworkController {
	import ListMarkSchemesController._
	
	@ModelAttribute("command") def command(@PathVariable("department") department: Department) = new Form(department)
	
	@RequestMapping
	def list(@ModelAttribute("command") form: Form): Mav = {
		Mav("admin/markschemes/list", 
		    "markSchemeInfo" -> form.apply())
		    .crumbsList(getCrumbs(form))
	}
	
	def getCrumbs(form: Form) = Seq (
		Breadcrumbs.Department(form.department)
	)
	
}

object ListMarkSchemesController {
	class Form(val department: Department) extends Command[Seq[Map[String, Any]]] with ReadOnly with Unaudited with Daoisms {
		PermissionCheck(Permissions.MarkScheme.Read(), department)
	
		var dao = Wire.auto[MarkSchemeDao]

		def applyInternal() = {
			val markSchemes = session.newCriteria[MarkScheme]
				.add(Restrictions.eq("department", department))
				.list
		  
			for (markScheme <- markSchemes) yield Map(
				"markScheme" -> markScheme,
				"assignmentCount" -> dao.getAssignmentsUsingMarkScheme(markScheme).size
			)
		}
	}
}