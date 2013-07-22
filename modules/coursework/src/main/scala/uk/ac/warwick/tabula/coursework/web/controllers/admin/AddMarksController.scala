package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.coursework.commands.assignments.AdminAddMarksCommand
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.services.FeedbackService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marks"))
class AddMarksController extends CourseworkController {

	@Autowired var feedbackService: FeedbackService = _
	@Autowired var assignmentMembershipService: AssignmentMembershipService = _

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new AdminAddMarksCommand(module, assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def viewMarkUploadForm(
			@PathVariable module: Module, 
			@PathVariable(value = "assignment") assignment: Assignment, 
			@ModelAttribute cmd: AdminAddMarksCommand, errors: Errors): Mav = {
		
		val members = assignmentMembershipService.determineMembershipUsers(cmd.assignment)

		val marksToDisplay = members.map { member =>
			val feedback = feedbackService.getStudentFeedback(assignment, member.getWarwickId)
			noteMarkItem(member, feedback)
		}

		crumbed(Mav("admin/assignments/marks/marksform", "marksToDisplay" -> marksToDisplay), cmd.module)

	}

	def noteMarkItem(member: User, feedback: Option[Feedback]) = {

		logger.debug("in noteMarkItem (logger.debug)")

		val markItem = new MarkItem()
		markItem.universityId = member.getWarwickId

		feedback match {
			case Some(f) => {
				markItem.actualMark = f.actualMark.map { _.toString }.getOrElse("")
				markItem.actualGrade = f.actualGrade.map { _.toString }.getOrElse("")
			}
			case None => {
				markItem.actualMark = ""
				markItem.actualGrade = ""
			}
		}

		markItem
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment, 
						   @ModelAttribute cmd: AdminAddMarksCommand, errors: Errors): Mav = {
		if (errors.hasErrors) viewMarkUploadForm(module, assignment, cmd, errors)
		else {
			bindAndValidate(module, cmd, errors)
			crumbed(Mav("admin/assignments/marks/markspreview"), module)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment, 
				 @ModelAttribute cmd: AdminAddMarksCommand, errors: Errors): Mav = {
		bindAndValidate(module, cmd, errors)
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

	private def bindAndValidate(module: Module, cmd: AdminAddMarksCommand, errors: Errors) {
		cmd.postExtractValidation(errors)
	}

}