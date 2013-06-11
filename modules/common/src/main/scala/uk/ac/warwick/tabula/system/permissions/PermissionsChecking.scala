package uk.ac.warwick.tabula.system.permissions

import org.springframework.util.Assert
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.CanBeDeleted
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.FeedbackTemplate
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.services.SecurityService
import scala.annotation.target

/**
 * Trait that allows classes to call ActionCheck() in their inline definitions
 * (i.e. on construction). These are then evaluated on bind.
 */
trait PermissionsChecking extends PermissionsCheckingMethods {

	var permissionsAnyChecks: Map[Permission, Option[PermissionsTarget]] = Map()
	var permissionsAllChecks: Map[Permission, Option[PermissionsTarget]] = Map()

	def PermissionCheckAny(checkablePermissions: => Iterable[CheckablePermission]) {
		for (p <- checkablePermissions) checkAny(p.permission, p.scope)
	}

	def PermissionCheckAll(permission: Permission, scopes: => Iterable[PermissionsTarget]) {
		for (scope <- scopes) checkAll(permission, Some(scope))
	}

	def PermissionCheck(scopelessPermission: ScopelessPermission) {
		checkAll(scopelessPermission, None)
	}

	def PermissionCheck(permission: Permission, scope: PermissionsTarget) {
		checkAll(permission, Some(scope))
	}

	private def checkAny(permission: Permission, scope: Option[PermissionsTarget]) {
		permissionsAnyChecks += (permission -> scope)
	}

	private def checkAll(permission: Permission, scope: Option[PermissionsTarget]) {
		permissionsAllChecks += (permission -> scope)
	}
}

trait Public extends PermissionsChecking

trait PermissionsCheckingMethods extends Logging {
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
	def mandatory[A : ClassTag](something: A): A = something match {
		case thing: A => thing
		case _ => throw new ItemNotFoundException()
	}
	/**
	 * Pass in an Option and receive either the actual value, or
	 * an ItemNotFoundException is thrown.
	 */
	def mandatory[A : ClassTag](option: Option[A]): A = option match {
		case Some(thing: A) => thing
		case _ => throw new ItemNotFoundException()
	}

	def notDeleted[A <: CanBeDeleted](entity: A): A =
		if (entity.deleted) throw new ItemNotFoundException()
		else entity

	/**
	 * Checks target.permissionsAllChecks for ANDed permission, then target.permissionsAnyChecks for ORed permissions.
	 * Throws PermissionDeniedException if permissions are unmet or ItemNotFoundException (-> 404) if scope is missing.
	 */
	def permittedByChecks(securityService: SecurityService, user: CurrentUser, target: PermissionsChecking) {
		Assert.isTrue(
			!target.permissionsAnyChecks.isEmpty || !target.permissionsAllChecks.isEmpty || target.isInstanceOf[Public],
			"Bind target " + target.getClass + " must specify permissions or extend Public"
		)

		// securityService.check() throws on *any* missing permission
		for (check <- target.permissionsAllChecks) check match {
			case (permission: Permission, Some(scope)) => securityService.check(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.check(user, permission)
			case _ =>
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
		}

		// securityService.can() wrapped in exists() only throws if no perms match
		if (!target.permissionsAnyChecks.isEmpty && !target.permissionsAnyChecks.exists ( _ match {
			case (permission: Permission, Some(scope)) => securityService.can(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.can(user, permission)
			case _ => {
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
			}
		})) throw new PermissionDeniedException(user, target.permissionsAnyChecks.head._1, target.permissionsAnyChecks.head._2)
	}
}