package uk.ac.warwick.courses.commands
import collection.mutable
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.JavaImports
import org.springframework.validation.Errors

/**
 * Trait for a thing that can describe itself to a Description
 * object. You can put arbitrary properties into Description but
 * it's always best to use a dedicated method, e.g. assignment(), to
 * make things more maintainable. assignment() will automatically
 * record its module and department info.
 */
trait Describable[T] {
	// describe the thing that's happening.
	def describe(d:Description)
	// optional extra description after the thing's happened.
	def describeResult(d:Description, result:T) { describeResult(d) }
	def describeResult(d:Description) {}
	val eventName:String
}

/**
 * Stateful instance of an action in the application.
 * It could be anything that we might want to keep track of,
 * especially if we might want to audit log it. Anything that
 * adds or changes any data is a candidate. Read-only queries,
 * not so much (unless we're interested in when a thing is observed/downloaded).
 * 
 * <h2>Renaming a Command</h2>
 * 
 * Think before renaming a command - by default the class name (minus "Command") is
 * used as the event name in audit trails, so if you rename it the audit events will
 * change name too. Careful now!
 */
trait Command[R] extends Describable[R] with JavaImports {
	def apply(): R
	lazy val eventName = getClass.getSimpleName.replaceAll("Command$","")
}

trait ApplyWithCallback[R] extends Command[R] {
	var callback: (R)=>Unit =_
	def apply(fn:(R)=>Unit) : R = {
		callback = fn
		apply
	}
}

trait SelfValidating {
	def validate(implicit errors:Errors)
	
	def reject(code:String)(implicit errors:Errors) = errors.reject(code)
	def rejectValue(prop:String, code:String)(implicit errors:Errors) = errors.rejectValue(prop,code)
}

/**
 * Marks a command as being safe to use during maintenance mode (other than audit events
 * which are handled separately). If it doesn't directly update or insert into the database,
 * it is safe.
 */
trait ReadOnly

trait Unaudited { self:Describable[_] =>
	// override describe() with nothing, since it'll never be used.
	override def describe(d:Description) {}
}

/**
 * Object for a Command to describe what it's just done.
 */
abstract class Description {
	protected var map = Map[String,Any]()
	def properties(props: Pair[String,Any]*) = {
		map ++= props
		this
	}
	def properties(otherMap: Map[String,Any]) = {
		map ++= otherMap
		this
	}
	def property(prop: Pair[String,Any]) = {
		map += prop
		this
	}
	
	/**
	 * Record a Feedback item, plus its assignment, module, department
	 */
	def feedback(feedback:Feedback) = {
		map += "feedback" -> feedback.id
		if (feedback.assignment != null) assignment(feedback.assignment)
		this
	}
	
	/**
	 * University IDs
	 */
	def studentIds(universityIds:Seq[String]) = property("students" -> universityIds)
	
	/**
	 * List of Submissions IDs
	 */
	def submissions(submissions:Seq[Submission]) = property("submissions" -> submissions.map(_.id))
	
	/**
	 * Record assignment, plus its module and department if available.
	 */
	def assignment(assignment:Assignment) = {
		property("assignment" -> assignment.id)
		if (assignment.module != null) module(assignment.module)
		this
	}
	
	/**
	 * Record module, plus department.
	 */
	def module(module:Module) = properties(
			"module" -> module.id,
			"department" -> module.department.code
		)
	
	// delegate equality to the underlying map
	override def hashCode = map.hashCode
	override def equals(that:Any) = that match {
		case d:Description => map.equals(d.map)
		case _ => false
	}
}

class DescriptionImpl extends Description {
	def allProperties = map
}