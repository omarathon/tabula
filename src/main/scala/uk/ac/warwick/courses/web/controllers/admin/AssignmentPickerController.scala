package uk.ac.warwick.courses.web.controllers.admin

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.JList
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMethod
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.courses.web.views.JSONView
import uk.ac.warwick.courses.actions.Participate

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/picker"))
class AssignmentPickerController extends BaseController {
    @Autowired var assignmentService:AssignmentService = _
    @Autowired var json:ObjectMapper =_
    
    @RequestMapping
    def submit(user: CurrentUser, @PathVariable module: Module,
            form: AssignmentPickerForm, response:HttpServletResponse, errors: Errors) = {
      
        mustBeAbleTo(Participate(module))

        val assignments = assignmentService.getAssignmentsByName(form.searchTerm, module.department)

        val assignmentsJson:JList[Map[String, Object]] = toJson(assignments)

        new JSONView(assignmentsJson)
    }
    
      
    def toJson(assignments:Seq[Assignment]) = {

        def assignmentToJson(assignment : Assignment) = Map[String, String](
            "name" -> assignment.name,
            "id" -> assignment.id,
            "moduleName" -> assignment.module.name,
            "moduleCode" -> assignment.module.code
            )

        val assignmentsJson = assignments.map(assignmentToJson(_))
        assignmentsJson
  }

}

class AssignmentPickerForm {
    @BeanProperty var searchTerm : String = ""
}
