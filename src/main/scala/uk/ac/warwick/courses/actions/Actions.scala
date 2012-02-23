package uk.ac.warwick.courses.actions
import uk.ac.warwick.courses.data.model.Assignment

sealed abstract class Action[T]

case class View(val item:Viewable) extends Action[Viewable]
case class Submit(val item:Assignment) extends Action[Assignment]
// At the moment, participating means viewing module overview, listing and uploading feedback.
case class Participate(val item:Participatable) extends Action[Participatable]
case class Manage(val item:Manageable) extends Action[Manageable]
// full-on sysadmin masquerading.
case class Masquerade() extends Action[Unit]

trait Viewable
trait Manageable
trait Participatable


object Action {
	
	private val PackagePrefix = Action.getClass.getPackage.getName + "."
	
	/**
	 * Create an Action from an action name (e.g. "View") and a target.
	 * Most likely useful in view templates, for permissions checking.
	 * 
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime. 
	 */
	def of(name:String, target:Object*) : Action[_] = {
		try {
			val clz = Class.forName(PackagePrefix+name).asSubclass(classOf[Action[_]])
			clz.getConstructors()(0).newInstance(target:_*).asInstanceOf[Action[_]]
		} catch {
			case e:ClassNotFoundException => throw new IllegalArgumentException("Action "+name+" not recognised")
		}
	}

}