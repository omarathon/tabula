package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.coursework.commands.assignments.AddMarksCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Participate
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Feedback

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marks"))
class AddMarksController extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser) = new AddMarksCommand(assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def uploadZipForm(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment, @ModelAttribute cmd: AddMarksCommand): Mav = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val members = assignmentService.determineMembershipUsers(assignment)

		var marksToDisplay = List[MarkItem]()

		logger.debug("sizeof marksToDisplay is " + marksToDisplay.size)

		members.foreach(member => {
			val feedback = assignmentService.getStudentFeedback(assignment, member.getWarwickId())
			//feedback.foreach(marksToDisplay ::= noteMarkItem(member, _))
			marksToDisplay ::= noteMarkItem(member, feedback)
		})

		crumbed(Mav("admin/assignments/marks/marksform", "marksToDisplay" -> marksToDisplay), module)

	}

	def noteMarkItem(member: User, feedback: Option[Feedback]) = {

		logger.debug("in noteMarkItem (logger.debug)")

		val markItem = new MarkItem()
		markItem.universityId = member.getWarwickId()

		feedback match {
			case Some(f) => {
				markItem.actualMark = f.actualMark.map { _.toString() }.getOrElse("")
				markItem.actualGrade = f.actualGrade
			}
			case None => {
				markItem.actualMark = ""
				markItem.actualGrade = ""
			}
		}

		markItem
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable module: Module, @PathVariable assignment: Assignment, @ModelAttribute cmd: AddMarksCommand, errors: Errors): Mav = {
		cmd.onBind
		cmd.postExtractValidation(errors)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		crumbed(Mav("admin/assignments/marks/markspreview"), module)
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module, @PathVariable assignment: Assignment, @ModelAttribute cmd: AddMarksCommand, errors: Errors): Mav = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		cmd.onBind
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}