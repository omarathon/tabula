package uk.ac.warwick.tabula.system.permissions

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.CanBeDeleted
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.FeedbackTemplate
import uk.ac.warwick.tabula.roles.Role
import scala.reflect.ClassTag

/**
 * Trait that allows classes to call ActionCheck() in their inline definitions 
 * (i.e. on construction). These are then evaluated on bind.
 */
trait PermissionsChecking extends PermissionsCheckingMethods {
	
	var permissionChecks: Map[Permission, Option[PermissionsTarget]] = Map()

	def PermissionCheckAll(permission: Permission, scopes: => Iterable[PermissionsTarget]) {
		for (scope <- scopes) check(permission, Some(scope))
	}
	
	def PermissionCheck(scopelessPermission: ScopelessPermission) {
		check(scopelessPermission, None)
	}
	
	def PermissionCheck(permission: Permission, scope: PermissionsTarget) {
		check(permission, Some(scope))
	}
	
	private def check(permission: Permission, scope: Option[PermissionsTarget]) {
		permissionChecks += (permission -> scope)
	}
	
}

trait Public extends PermissionsChecking

abstract trait PermissionsCheckingMethods extends Logging {
	def mustBeLinked(assignment: Assignment, module: Module) =
		if (mandatory(assignment).module.id != mandatory(module).id) {
			logger.info("Not displaying assignment as it doesn't belong to specified module")
			throw new ItemNotFoundException(assignment)
		}
	
	def mustBeLinked(set: SmallGroupSet, module: Module) =
		if (mandatory(set).module.id != mandatory(module).id) {
			logger.info("Not displaying small group set as it doesn't belong to specified module")
			throw new ItemNotFoundException(set)
		}

	def mustBeLinked(feedback: Feedback, assignment: Assignment) =
		if (mandatory(feedback).assignment.id != mandatory(assignment).id) {
			logger.info("Not displaying feedback as it doesn't belong to specified assignment")
			throw new ItemNotFoundException(feedback)
		}
	
	def mustBeLinked(markingWorkflow: MarkingWorkflow, department: Department) =
		if (mandatory(markingWorkflow).department.id != mandatory(department.id)) {
			logger.info("Not displaying marking workflow as it doesn't belong to specified department")
			throw new ItemNotFoundException(markingWorkflow)
		}
	
	def mustBeLinked(template: FeedbackTemplate, department: Department) =
		if (mandatory(template).department.id != mandatory(department.id)) {
			logger.info("Not displaying feedback template as it doesn't belong to specified department")
			throw new ItemNotFoundException(template)
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
	def mandatory[A](something: A)(implicit tag: ClassTag[A]): A = something match {
		case thing: Any if tag.runtimeClass.isInstance(thing) => thing.asInstanceOf[A]
		case _ => throw new ItemNotFoundException()
	}
	/**
	 * Pass in an Option and receive either the actual value, or
	 * an ItemNotFoundException is thrown.
	 */
	def mandatory[A](option: Option[A])(implicit tag: ClassTag[A]): A = option match {
		case Some(thing: Any) if tag.runtimeClass.isInstance(thing) => thing.asInstanceOf[A]
		case _ => throw new ItemNotFoundException()
	}

	def notDeleted[A <: CanBeDeleted](entity: A): A =
		if (entity.deleted) throw new ItemNotFoundException()
		else entity
}