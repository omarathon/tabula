package uk.ac.warwick.tabula.coursework.commands

import scala.annotation.target._
import collection.mutable
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.coursework.data.model._
import uk.ac.warwick.tabula.coursework.events.EventHandling
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.coursework.services.MaintenanceModeService
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire

/**
 * Trait for a thing that can describe itself to a Description
 * object. You can put arbitrary properties into Description but
 * it's always best to use a dedicated method, e.g. assignment(), to
 * make things more maintainable. assignment() will automatically
 * record its module and department info.
 */
trait Describable[T] {
	// describe the thing that's happening.
	def describe(d: Description)
	// optional extra description after the thing's happened.
	def describeResult(d: Description, result: T) { describeResult(d) }
	def describeResult(d: Description) {}
	val eventName: String
}

/**
 * Stateful instance of an action in the application.
 * It could be anything that we might want to keep track of,
 * especially if we might want to audit log it. Anything that
 * adds or changes any data is a candidate. Read-only queries,
 * not so much (unless we're interested in when a thing is observed/downloaded).
 * 
 * Commands should implement work(), and 
 *
 * <h2>Renaming a Command</h2>
 *
 * Think before renaming a command - by default the class name (minus "Command") is
 * used as the event name in audit trails, so if you rename it the audit events will
 * change name too. Careful now!
 */
abstract class Command[R] extends Describable[R] with JavaImports with EventHandling {
	var maintenanceMode = Wire.auto[MaintenanceModeService]

	final def apply(): R = {
		if (EventHandling.enabled) {
			if (maintenanceCheck(this)) recordEvent(this) { work() }
			else throw maintenanceMode.exception()
		} else {
			work()
		}
	} 

	/** 
		Subclasses do their work in here.

		Classes using a command should NOT call this method! call apply().
		The method here is protected but subclasses can easily override it
		to be publicly visible, so there's little to stop you from calling it.
		TODO somehow stop this being callable
	*/
	protected def work(): R

	lazy val eventName = getClass.getSimpleName.replaceAll("Command$", "")

	private def maintenanceCheck(callee: Describable[_]) = {
		callee.isInstanceOf[ReadOnly] || !maintenanceMode.enabled
	}
}

/**
 * Defines a function property to be used as a callback, plus a convenience
 * version of `apply` that provides the callback and runs the command
 * simultaneously.
 *
 * It doesn't actually call the callback - you do that in your `apply` implementation.
 */
trait ApplyWithCallback[R] extends Command[R] {
	var callback: (R) => Unit = _
	def apply(fn: (R) => Unit): R = {
		callback = fn
		apply()
	}
}

/**
 * Trait for a command which has a `validate` method. Implementing this trait
 * doesn't actually make anything magic happen at the moment - you still have
 * to call the validate method yourself. It does provide a few shortcuts to the
 * validation methods to simplify validation code.
 */
trait SelfValidating {
	def validate(implicit errors: Errors)

	def reject(code: String)(implicit errors: Errors) = errors.reject(code)
	def rejectValue(prop: String, code: String)(implicit errors: Errors) = errors.rejectValue(prop, code)
}

/**
 * Marks a command as being safe to use during maintenance mode (other than audit events
 * which are handled separately). If it doesn't directly update or insert into the database,
 * it is safe.
 */
trait ReadOnly

/**
 * A Describable (usually a Command) marked as Unaudited will not be recorded
 * by the audit log when it is applied. This should only really be for read-only
 * commands that make no database changes and are really uninteresting to log, like
 * viewing a list of items.
 */
trait Unaudited { self: Describable[_] =>
	// override describe() with nothing, since it'll never be used.
	override def describe(d: Description) {}
}

/**
 * Object for a Command to describe what it's just done.
 *
 * You can use the `properties` and `property` methods to add any
 * arbitrary properties, but it's highly recommended that you use the
 * dedicated methods such as `assignment` to record which assignment
 * the command is working on, and to define a new method if the
 * existing ones don't fulfil your needs.
 */
abstract class Description {
	protected var map = Map[String, Any]()
	def properties(props: Pair[String, Any]*) = {
		map ++= props
		this
	}
	def properties(otherMap: Map[String, Any]) = {
		map ++= otherMap
		this
	}
	def property(prop: Pair[String, Any]) = {
		map += prop
		this
	}

	/**
	 * Record a Feedback item, plus its assignment, module, department
	 */
	def feedback(feedback: Feedback) = {
		map += "feedback" -> feedback.id
		if (feedback.assignment != null) assignment(feedback.assignment)
		this
	}

	/**
	 * University IDs
	 */
	def studentIds(universityIds: Seq[String]) = property("students" -> universityIds)

	/**
	 * List of Submissions IDs
	 */
	def submissions(submissions: Seq[Submission]) = property("submissions" -> submissions.map(_.id))

	/**
	 * Record assignment, plus its module and department if available.
	 */
	def assignment(assignment: Assignment) = {
		property("assignment" -> assignment.id)
		if (assignment.module != null) module(assignment.module)
		this
	}

	/**
	 * Record module, plus department.
	 */
	def module(module: Module) = {
		property("module" -> module.id)
		if (module.department != null) department(module.department)
		this
	}

	def department(department: Department) = {
		property("department", department.code)
		this
	}

	// delegate equality to the underlying map
	override def hashCode = map.hashCode
	override def equals(that: Any) = that match {
		case d: Description => map.equals(d.map)
		case _ => false
	}
}

/**
 * Fully implements Description, adding an accessor
 * to the underlying properties map for the auditing
 * framework to use.
 */
class DescriptionImpl extends Description {
	def allProperties = map
}