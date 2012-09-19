package uk.ac.warwick.courses.web.controllers

import uk.ac.warwick.courses._
import data.model.Assignment
import actions.Action
import system.exceptions.UserError

/**
 * Specific exception for when a student/person is not allowed to view
 * the submission/feedback/info page for an assignment. It is just so the
 * exception resolver can send it off to a specific error page.
 */
class SubmitPermissionDeniedException(assignment: Assignment) extends RuntimeException() with UserError
