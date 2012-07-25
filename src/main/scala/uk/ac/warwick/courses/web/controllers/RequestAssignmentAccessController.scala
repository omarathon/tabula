package uk.ac.warwick.courses.web.controllers

import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.assignments.RequestAssignmentAccessCommand
import uk.ac.warwick.courses.web.{Mav, Routes}
import uk.ac.warwick.courses.CurrentUser

@Configurable
@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/request-access"))
class RequestAssignmentAccessController extends AbstractAssignmentController {

  hideDeletedItems

  @ModelAttribute def cmd(user:CurrentUser) = new RequestAssignmentAccessCommand(user)

  @RequestMapping(method = Array(GET, HEAD))
  def nope(form: RequestAssignmentAccessCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

  @RequestMapping(method = Array(POST))
  def sendEmail(user: CurrentUser, form: RequestAssignmentAccessCommand): Mav = {
    mustBeLinked(form.assignment, form.module)
    form.apply()
    Redirect(Routes.assignment(form.assignment)).addObjects("requestedAccess" -> true)
  }

}
