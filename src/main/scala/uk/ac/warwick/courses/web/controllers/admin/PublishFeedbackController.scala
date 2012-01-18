package uk.ac.warwick.courses.web.controllers.admin
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.commands.PublishFeedbackCommand
import uk.ac.warwick.courses.actions.Participate
import org.springframework.validation.Errors

@RequestMapping(value=Array("/admin/module/${module}/assignments/{assignment}/publish"))
@Controller
class PublishFeedbackController extends BaseController {

  private def check(command:PublishFeedbackCommand) {
    mustBeLinked(command.assignment, command.module)
	mustBeAbleTo(Participate(command.assignment.module))
  }
    
  @RequestMapping(params=Array("!confirm"))
  def confirmation(command:PublishFeedbackCommand, errors:Errors): Mav = {
    check(command)
    command.prevalidate(errors)
    Mav("admin/assignments/publish/form", 
        "assignment" -> command.assignment)
  }
  
  @RequestMapping(method=Array(POST))
  def submit(command:PublishFeedbackCommand, errors:Errors): Mav = {
    check(command)
    command.validate(errors)
    if (errors.hasErrors()) {
    	confirmation(command, errors)
    } else {
	    command.apply
	    Mav("admin/assignments/publish/done", "assignment" -> command.assignment)
    }
  }
  
}