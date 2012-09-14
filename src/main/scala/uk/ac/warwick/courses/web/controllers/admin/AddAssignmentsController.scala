package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.commands.assignments.AddAssignmentsCommand
import uk.ac.warwick.courses
import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.data.model.Department
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.joda.time.DateTime
import collection.JavaConversions._
import org.springframework.validation.Errors
import javax.validation.Valid
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.courses._

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
@RequestMapping(value=Array("/admin/department/{department}/setup-assignments"))
class AddAssignmentsController extends BaseController {

	validatesSelf[AddAssignmentsCommand]

	// The initial load of page 1, where we select the items to import.
	@RequestMapping(method=Array(GET))
	def selectionForm(@ModelAttribute cmd:AddAssignmentsCommand, errors: Errors): Mav = {
		checkPermissions(cmd)
		cmd.populateWithItems()
		getMav(cmd).addObjects("action" -> "select")
	}

	// Reloads page 1 with a POST, to show any updated information if necessary.
	@RequestMapping(method=Array(POST), params=Array("action=refresh-select"))
	def refreshSelectionForm(@ModelAttribute cmd:AddAssignmentsCommand, errors: Errors): Mav = {
		checkPermissions(cmd)
		getMav(cmd).addObjects("action" -> "select")
	}

	// Loads page 2 where we set options on all the assignments.
	@RequestMapping(method=Array(POST), params=Array("action=options"))
	def optionsForm(@ModelAttribute cmd:AddAssignmentsCommand, errors: Errors): Mav = {
		checkPermissions(cmd)
		getMav(cmd).addObjects("action" -> "options")
	}
	
	// Do validation and return as a chunk of HTML errors.
    @RequestMapping(method=Array(POST), params=Array("action=validate"))
    def ajaxValidation(@Valid @ModelAttribute cmd:AddAssignmentsCommand, errors: Errors): Mav = {
    	Mav("admin/assignments/batch_new_validation").noLayout()
    }

	// Final step where we actually do the work.
	@RequestMapping(method=Array(POST), params=Array("action=submit"))
	def submit(@Valid @ModelAttribute cmd:AddAssignmentsCommand, errors: Errors): Mav = {
		checkPermissions(cmd)
		if (errors.hasErrors()) { 
			optionsForm(cmd, errors)
		} else {
		    cmd.apply()
    		Mav("admin/assignments/batch_new_done") // FIXME stupid view
    		    .crumbs(Breadcrumbs.Department(cmd.department))
		}
	}

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(0,1)
	}

	@ModelAttribute def cmd(department: Department) = {
		new AddAssignmentsCommand(department)
	}

	// The shared Mav for most of the request mappings
	def getMav(cmd:AddAssignmentsCommand) = {
		Mav("admin/assignments/batch_new_select")
			.crumbs(Breadcrumbs.Department(cmd.department))
	}

	def checkPermissions(cmd:AddAssignmentsCommand) = {
		mustBeAbleTo(Manage(cmd.department))

		// check that all the selected items are part of this department. Otherwise you could post the IDs of
		// unrelated assignments and do stuff with them.
		// Use .exists() to see if there is at least one with a matching department code
		val hasInvalidAssignments = cmd.assignmentItems.exists { (item) =>
			item.upstreamAssignment.departmentCode.toLowerCase != cmd.department.code
		}
		if (hasInvalidAssignments) {
			logger.warn("Rejected request to setup assignments that aren't in this department")
			throw new PermissionDeniedException(user, Manage(cmd.department))
		}
	}

}
