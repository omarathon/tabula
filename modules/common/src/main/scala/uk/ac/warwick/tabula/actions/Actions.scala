package uk.ac.warwick.tabula.actions

import uk.ac.warwick.tabula.data.model.Assignment

/**
 * A hierarchy of classes to represent actions within the system,
 * mainly for permissions purposes. The security service accepts actions
 * and decides whether the current user should be able to perform it.
 * <p>
 * Actions are part of the permissions system. They are passed along with
 * a user to the [[uk.ac.warwick.tabula.services.SecurityService]] which will
 * check whether that user can perform that action. The Action instance
 * typically has a reference to the object it is acting on.
 */
sealed abstract class Action[T]

case class View(val item: Viewable) extends Action[Viewable]

case class Search(val clazz: Class[_ <: Searchable]) extends Action[Searchable]

case class Submit(val item: Assignment) extends Action[Assignment]

case class DownloadSubmissions(val item: Assignment) extends Action[Assignment]

case class UploadMarkerFeedback(val item: Assignment) extends Action[Assignment]

/**
 * At the moment, participating means you can do most admin actions within this object, but
 * stops short of [Manage]-level things such as editing permissions.
 */
case class Participate(val item: Participatable) extends Action[Participatable]

/** Managing means pretty much full control of a thing - you can edit its details, change its permissions. */
case class Manage(val item: Manageable) extends Action[Manageable]

/** Whether you're allowed to masquerade as any user. */
case class Masquerade() extends Action[Unit]

case class Delete(val d: Deleteable) extends Action[Deleteable]

case class Create() extends Action[Unit]

/** Applied to any object that can have the View action on it. */
trait Viewable
/** Applied to any class of object that can have the Search action on it. */
trait Searchable
/** Applied to any object that can have the Manage action on it. */
trait Manageable
/** Applied to any object that can have the Participate action on it. */
trait Participatable
/** Applied to any object that can have the Delete action on it. */
trait Deleteable

object Action {

	private val PackagePrefix = Action.getClass.getPackage.getName + "."

	/**
	 * Create an Action from an action name (e.g. "View") and a target.
	 * Most likely useful in view templates, for permissions checking.
	 *
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime.
	 */
	def of(name: String, target: Object*): Action[_] = {
		try {
			val clz = Class.forName(PackagePrefix + name).asSubclass(classOf[Action[_]])
			clz.getConstructors()(0).newInstance(target: _*).asInstanceOf[Action[_]]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Action " + name + " not recognised")
		}
	}

}