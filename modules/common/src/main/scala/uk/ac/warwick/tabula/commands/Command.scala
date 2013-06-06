package uk.ac.warwick.tabula.commands

import scala.annotation.target._
import collection.mutable
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.{NotificationHandling, EventHandling, Event, EventDescription}
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.services.MaintenanceModeService
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.NoBind
import uk.ac.warwick.tabula.helpers.Stopwatches.StopWatch
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.helpers.Promises

/**
 * Trait for a thing that can describe itself to a Description
 * object. You can put arbitrary properties into Description but
 * it's always best to use a dedicated method, e.g. assignment(), to
 * make things more maintainable. assignment() will automatically
 * record its module and department info.
 */
trait Describable[A] {
	// describe the thing that's happening.
	def describe(d: Description)
	// optional extra description after the thing's happened.
	def describeResult(d: Description, result: A) { describeResult(d) }
	def describeResult(d: Description) {}
	val eventName: String
}

trait NotificationSource[A] {
	def emit: Notification[A]
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
abstract class Command[A] extends Describable[A]
with JavaImports with EventHandling with NotificationHandling with PermissionsChecking {
	var maintenanceMode = Wire[MaintenanceModeService]
	
	import uk.ac.warwick.tabula.system.NoBind

	final def apply(): A = {
		if (EventHandling.enabled) {
			if (maintenanceCheck(this))
				recordEvent(this) { notify(this) { benchmarkTask(benchmarkDescription) { applyInternal() } } }
			else throw maintenanceMode.exception()
		} else {
			benchmarkTask(benchmarkDescription) { applyInternal() }
		}
	}
	
	protected final def benchmarkTask[A](description: String)(fn: => A): A = Command.timed { timer =>
		benchmark(description, level=Warn, minMillis=Command.MillisToSlowlog, stopWatch=timer, logger=Command.slowLogger)(fn)
	}
	
	private def benchmarkDescription = {
		val event = Event.fromDescribable(this)
		EventDescription.generateMessage(event, "command").toString
	}

	/** 
		Subclasses do their work in here.

		Classes using a command should NOT call this method! call apply().
		The method here is protected but subclasses can easily override it
		to be publicly visible, so there's little to stop you from calling it.
		TODO somehow stop this being callable
	*/
	protected def applyInternal(): A

	lazy val eventName = getClass.getSimpleName.replaceAll("Command$", "")

	private def maintenanceCheck(callee: Describable[_]) = {
		callee.isInstanceOf[ReadOnly] || !maintenanceMode.enabled
	}
}

abstract class PromisingCommand[A] extends Command[A] with Promise[A] {
	private var _promise = Promises.promise[A]
	
	final def get = promisedValue
	final def promisedValue = _promise.get
	final def promisedValue_=(value: => A) = {
		_promise.set(value)
		value
	}
}

object Command {
	val MillisToSlowlog = 5000
	val slowLogger = Logger.getLogger("uk.ac.warwick.tabula.Command.SLOW_LOG")
	
	// TODO this will break if we start doing stuff in parallols
	private val threadLocal = new ThreadLocal[Option[uk.ac.warwick.util.core.StopWatch]] {
		override def initialValue = None
	}
	
	def timed[A](fn: uk.ac.warwick.util.core.StopWatch => A): A = {
		val currentStopwatch = threadLocal.get
		if (!currentStopwatch.isDefined) {
			try {
				val sw = StopWatch()
				threadLocal.set(Some(sw))
				fn(sw)
			} finally {
				threadLocal.remove
			}
		} else {
			fn(currentStopwatch.get)
		}
	}
}

/**
 * Defines a function property to be used as a callback, plus a convenience
 * version of `apply` that provides the callback and runs the command
 * simultaneously.
 *
 * It doesn't actually call the callback - you do that in your `apply` implementation.
 */
trait ApplyWithCallback[A] extends Command[A] {
	var callback: (A) => Unit = _
	def apply(fn: (A) => Unit): A = {
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
	def validate(errors: Errors)
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
     * Record a Submission item, plus its assignment, module, department
     */
    def submission(submission: Submission) = {
        map += "submission" -> submission.id
        if (submission.assignment != null) assignment(submission.assignment)
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
	 * Record small group set, plus its module and department if available.
	 */
	def smallGroupSet(smallGroupSet: SmallGroupSet) = {
		property("smallGroupSet" -> smallGroupSet.id)
		if (smallGroupSet.module != null) module(smallGroupSet.module)
		this
	}
	
	/**
	 * Record small group, plus its set, module and department if available.
	 */
	def smallGroup(smallGroup: SmallGroup) = {
		property("smallGroup" -> smallGroup.id)
		if (smallGroup.groupSet != null) smallGroupSet(smallGroup.groupSet)
		this
	}
	
	/**
	 * Record small group event, plus its group, set, module and department if available.
	 */
	def smallGroupEvent(smallGroupEvent: SmallGroupEvent) = {
		property("smallGroupEvent" -> smallGroupEvent.id)
		if (smallGroupEvent.group != null) smallGroup(smallGroupEvent.group)
		this
	}
	
	def markingWorkflow(scheme: MarkingWorkflow) = {
		property("markingWorkflow" -> scheme.id)
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

	def member(member: Member) = {
		property("member", member.universityId)
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
