package uk.ac.warwick.tabula.coursework.web.controllers.admin


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkerAddMarksCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.{UserLookupService, AssignmentService}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marks"))
class MarkerAddMarksController extends CourseworkController {

	@Autowired var assignmentService: AssignmentService = _
	@Autowired var userLookup: UserLookupService = _

	@ModelAttribute def command(@PathVariable("module") module: Module,
	                            @PathVariable("assignment") assignment: Assignment,
	                            user: CurrentUser) =
		new MarkerAddMarksCommand(module, assignment, user, assignment.isFirstMarker(user.apparentUser))

	@RequestMapping(method = Array(HEAD, GET))
	def viewMarkUploadForm(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment,
	             		   @ModelAttribute cmd: MarkerAddMarksCommand, errors: Errors): Mav = {

		val submissions = assignment.getMarkersSubmissions(user.apparentUser)
		val markerFeedbacks = submissions.flatMap(s => assignment.getMarkerFeedback(s.universityId, user.apparentUser))
		val filteredFeedbackId = markerFeedbacks.filter(_.state != MarkingCompleted).map(_.feedback.universityId)
		val filteredSubmissions = submissions.filter(s => filteredFeedbackId.contains(s.universityId))

		val marksToDisplay:Seq[MarkItem] = filteredSubmissions.map{ submission =>
			val universityId = submission.universityId
			val member = userLookup.getUserByWarwickUniId(universityId)

			val markerFeedback = markerFeedbacks.find(_.feedback.universityId == universityId)
			markerFeedback match  {
				case Some(f) if f.state != MarkingCompleted => noteMarkItem(member, Option(f))
				case None => noteMarkItem(member, None)
			}
		}

		Mav("admin/assignments/markerfeedback/marksform", "marksToDisplay" -> marksToDisplay)
	}

	def noteMarkItem(member: User, markerFeedback: Option[MarkerFeedback]) = {
		val markItem = new MarkItem()
		markItem.universityId = member.getWarwickId
		markerFeedback match {
			case Some(f) => {
				markItem.actualMark = f.mark.map { _.toString }.getOrElse("")
				markItem.actualGrade = f.grade.getOrElse("")
			}
			case None => {
				markItem.actualMark = ""
				markItem.actualGrade = ""
			}
		}
		markItem
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable module: Module,  @PathVariable(value = "assignment") assignment: Assignment,
	                       @ModelAttribute cmd: MarkerAddMarksCommand, errors: Errors) = {
		if (errors.hasErrors) viewMarkUploadForm(module, assignment, cmd, errors)
		else {
			bindAndValidate(assignment, cmd, errors)
			Mav("admin/assignments/markerfeedback/markspreview")
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module,  @PathVariable(value = "assignment") assignment: Assignment,
	             @ModelAttribute cmd: MarkerAddMarksCommand, errors: Errors) = {
		bindAndValidate(assignment, cmd, errors)
		cmd.apply()
		Redirect(Routes.admin.assignment.markerFeedback(assignment))
	}

	private def bindAndValidate(assignment: Assignment, cmd: MarkerAddMarksCommand, errors: Errors) {
		cmd.postExtractValidation(errors)
	}
}
