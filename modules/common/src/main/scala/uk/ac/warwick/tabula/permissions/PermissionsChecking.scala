package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.actions.Action
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.MarkScheme
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.CanBeDeleted
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.actions.UniversityMember

/**
 * Trait that allows classes to call PermissionsCheck() in their inline definitions 
 * (i.e. on construction). These are then evaluated on bind.
 */
trait PermissionsChecking extends PermissionsCheckingMethods {
	
	var permissionsChecks: Set[Action[_]] = Set()
		
	def PermissionsCheck(action: Action[_]) { 
		permissionsChecks += action
	}

}

trait Public extends PermissionsChecking

trait AllUniversityMembers extends PermissionsChecking {
		PermissionsCheck(UniversityMember())
}

abstract trait PermissionsCheckingMethods extends Logging {
	def mustBeLinked(assignment: Assignment, module: Module) =
		if (mandatory(assignment).module.id != mandatory(module).id) {
			logger.info("Not displaying assignment as it doesn't belong to specified module")
			throw new ItemNotFoundException(assignment)
		}

	def mustBeLinked(feedback: Feedback, assignment: Assignment) =
		if (mandatory(feedback).assignment.id != mandatory(assignment).id) {
			logger.info("Not displaying feedback as it doesn't belong to specified assignment")
			throw new ItemNotFoundException(feedback)
		}
	
	def mustBeLinked(markScheme: MarkScheme, department: Department) =
		if (mandatory(markScheme).department.id != mandatory(department.id)) {
			logger.info("Not displaying mark scheme as it doesn't belong to specified department")
			throw new ItemNotFoundException(markScheme)
		}

  def mustBeLinked(submission: Submission, assignment: Assignment) =
    if (mandatory(submission).assignment.id != mandatory(assignment).id) {
      logger.info("Not displaying submission as it doesn't belong to specified assignment")
      throw new ItemNotFoundException(submission)
    }	
	
	/**
	 * Returns an object if it is non-null and not None. Otherwise
	 * it throws an ItemNotFoundException, which should get picked
	 * up by an exception handler to display a 404 page.
	 */
	def mandatory[T](something: T)(implicit m: Manifest[T]): T = something match {
		case thing: Any if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		case _ => throw new ItemNotFoundException()
	}
	/**
	 * Pass in an Option and receive either the actual value, or
	 * an ItemNotFoundException is thrown.
	 */
	def mandatory[T](option: Option[T])(implicit m: Manifest[T]): T = option match {
		case Some(thing: Any) if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		case _ => throw new ItemNotFoundException()
	}

	def notDeleted[T <: CanBeDeleted](entity: T): T =
		if (entity.deleted) throw new ItemNotFoundException()
		else entity
}