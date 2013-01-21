package uk.ac.warwick.tabula

import data.model.Assignment
import actions.Action
import uk.ac.warwick.tabula.system.exceptions.UserError

/**
 * Specific exception for when a student/person is not allowed to view
 * the submission/feedback/info page for an assignment. It is just so the
 * exception resolver can send it off to a specific error page.
 */
class SubmitPermissionDeniedException(assignment: Assignment) extends RuntimeException() with UserError
