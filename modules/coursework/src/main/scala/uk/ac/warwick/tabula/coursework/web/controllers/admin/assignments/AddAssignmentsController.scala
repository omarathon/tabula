package uk.ac.warwick.tabula.coursework.web.controllers.admin.assignments

import uk.ac.warwick.tabula.coursework.commands.assignments.AddAssignmentsCommand
import uk.ac.warwick.tabula
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ ModelAttribute, RequestMapping }
import org.joda.time.DateTime
import collection.JavaConversions._
import org.springframework.validation.Errors
import javax.validation.Valid
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.CurrentUser

/**
 * Controller that handles the multi-step process of creating many assignments from SITS data.
 *
 * - selectionForm() is called which displays items with checkboxes to choose which assignments
 *      to import.
 * - submit to optionsForm() which is a similar form where we select assignments and set options on them
 * - AssignmentSharedOptionsController handles the options screen, and when that passes validation we
 *   copy all the form fields into the main page using javascript.
 * - To set dates we open a static popup which then copies the values into place with javascript
 * - Before submitting we make an AJAX call to ajaxValidation() to display any errors.
 * - Finally we submit everything to submit().
 */
@Controller
@RequestMapping(value = Array("/admin/department/{department}/setup-assignments"))
class AddAssignmentsController extends CourseworkController {

	validatesSelf[AddAssignmentsCommand]

	// The initial load of page 1, where we select the items to import.
	@RequestMapping(method = Array(GET))
	def selectionForm(@ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		cmd.afterBind()
		cmd.populateWithItems()
		getMav(cmd).addObjects("action" -> "select")
	}

	// Change the academic year; restarts from scratch
	@RequestMapping(method = Array(POST), params = Array("action=change-year"))
	def changeYear(@ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = selectionForm(cmd, errors)

	// Reloads page 1 with a POST, to show any updated information if necessary.
	@RequestMapping(method = Array(POST), params = Array("action=refresh-select"))
	def refreshSelectionForm(@ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		cmd.afterBind()
//		cmd.populateWithMissingItems()
		getMav(cmd).addObjects("action" -> "select")
	}

	// Loads page 2 where we set options on all the assignments.
	@RequestMapping(method = Array(POST), params = Array("action=options"))
	def optionsForm(@ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		cmd.afterBind()
		cmd.validateNames(errors)
		getMav(cmd).addObjects("action" -> "options")
	}

	// Do validation and return as a chunk of HTML errors.
	@RequestMapping(method = Array(POST), params = Array("action=validate"))
	def ajaxValidation(@Valid @ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		Mav("admin/assignments/batch_new_validation").noLayout()
	}

	// Final step where we actually do the work.
	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(@Valid @ModelAttribute cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		cmd.afterBind()
		if (errors.hasErrors()) {
			optionsForm(cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(cmd.department))
		}
	}

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(0, 1)
	}

	@ModelAttribute def cmd(department: Department, user: CurrentUser) = {
		new AddAssignmentsCommand(department, user)
	}

	// The shared Mav for most of the request mappings
	def getMav(cmd: AddAssignmentsCommand) = {
		Mav("admin/assignments/batch_new_select")
			.crumbs(Breadcrumbs.Department(cmd.department))
	}

}
