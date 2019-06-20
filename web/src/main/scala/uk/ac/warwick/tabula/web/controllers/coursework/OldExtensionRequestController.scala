package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(value = Array("/${cm1.prefix}/module/{module}/{assignment}/extension"))
class OldExtensionRequestController extends OldCourseworkController {

  @RequestMapping
  def redirectToCM2(@PathVariable assignment: Assignment): Mav = Redirect(Routes.extensionRequest(assignment))

}
