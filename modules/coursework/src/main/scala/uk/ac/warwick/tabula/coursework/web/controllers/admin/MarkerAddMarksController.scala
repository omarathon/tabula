package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.{AdminAddMarksCommand, MarkerAddMarksCommand}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.actions.Participate
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

	val isFirstMarker = true //TODO - change this depending on which marker this is

	@ModelAttribute def command(@PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new MarkerAddMarksCommand(assignment, user, isFirstMarker)

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment,
	             @ModelAttribute cmd: MarkerAddMarksCommand): Mav = {

		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val marksToDisplay:Seq[MarkItem] = submissions.map { submission =>
			val universityId = submission.universityId
			val member = userLookup.getUserByWarwickUniId(universityId)
			val feedback = assignmentService.getStudentFeedback(assignment, universityId)
			feedback match {
				case Some(f) => {
					val markerFeedback = isFirstMarker match {
						case true => f.firstMarkerFeedback
						case false => f.secondMarkerFeedback
						case _ => throw new IllegalStateException("isFirstMarker must be true or false")
					}
					noteMarkItem(member, Option(markerFeedback))
				}
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
				markItem.actualGrade = ""
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
		bindAndValidate(module, cmd, errors)
		Mav("admin/assignments/markerfeedback/markspreview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module,  @PathVariable(value = "assignment") assignment: Assignment,
	             @ModelAttribute cmd: MarkerAddMarksCommand, errors: Errors) = {
		bindAndValidate(module, cmd, errors)
		cmd.apply()
		Redirect(Routes.admin.assignment.markerFeedback(assignment))
	}

	private def bindAndValidate(module: Module, cmd: MarkerAddMarksCommand, errors: Errors) {
		mustBeLinked(cmd.assignment, module)
		mustBeAbleTo(Participate(module))
		cmd.onBind
		cmd.postExtractValidation(errors)
	}
}
