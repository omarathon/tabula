package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.exceptions.UserError
import uk.ac.warwick.tabula.data.model.Assignment

/**
 * Trait for matching any kind of permissions exception; mostly to force login for users who aren't logged in
 */
trait PermissionsError

/**
 * Thrown by security when a user tries to do an action that
 * they don't have permission to do. The app should handle
 * this exception specially and return a generic page (or perhaps
 * a more specific explanation page for certain situations).
 *
 * It shouldn't generally be possible to get this exception by
 * clicking in the UI. You'd only get this by trying to manually
 * go to a different department's URL, for example, or using a
 * bookmarked link to a department who you've since been removed from.
 *
 * Is a RuntimeException because we don't expect the app to need
 * to declare that this is thrown.
 */
class PermissionDeniedException(
	val user: CurrentUser,
	val permission: Permission,
	val scope: Any,
	cause: Throwable = null) extends RuntimeException(cause) with UserError with PermissionsError
	
/**
 * Specific exception for when a student/person is not allowed to view
 * the submission/feedback/info page for an assignment. It is just so the
 * exception resolver can send it off to a specific error page.
 */
class SubmitPermissionDeniedException(assignment: Assignment) extends RuntimeException() with UserError with PermissionsError