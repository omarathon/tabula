package uk.ac.warwick.courses.web.controllers.admin.markschemes

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.MarkScheme
import uk.ac.warwick.courses.web.Breadcrumbs
import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.courses.data.MarkSchemeDao

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes"))
class ListMarkSchemesController extends BaseController with Daoisms {
	import ListMarkSchemesController._
	
	var dao = Wire.auto[MarkSchemeDao]
	
	@RequestMapping
	def list(@ModelAttribute("command") form: Form): Mav = {
		mustBeAbleTo(Manage(form.department))
		val markSchemes = session.newCriteria[MarkScheme]
		  .add(Restrictions.eq("department", form.department))
		  .list
		  
		val markSchemeInfo = for (markScheme <- markSchemes) yield Map(
					"markScheme" -> markScheme,
					"assignmentCount" -> dao.getAssignmentsUsingMarkScheme(markScheme).size
				)
		
		  
		Mav("admin/markschemes/list", 
		    "markSchemeInfo" -> markSchemeInfo)
		    .crumbsList(getCrumbs(form))
	}
	
	def getCrumbs(form: Form) = Seq (
		Breadcrumbs.Department(form.department)
	)
	
}

object ListMarkSchemesController {
	class Form {
		@BeanProperty var department: Department = _
	}
}