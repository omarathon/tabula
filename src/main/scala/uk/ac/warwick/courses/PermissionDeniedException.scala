package uk.ac.warwick.courses
import uk.ac.warwick.courses.actions.Action

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
    val user:CurrentUser, 
    val action:Action[_], 
    cause:Throwable = null ) extends RuntimeException(cause) with UserError