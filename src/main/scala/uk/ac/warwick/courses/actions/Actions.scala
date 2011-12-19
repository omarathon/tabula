package uk.ac.warwick.courses.actions
import uk.ac.warwick.courses.data.model.Assignment

sealed abstract class Action[T]

case class View(val item:Viewable) extends Action[Viewable]
case class Submit(val item:Assignment) extends Action[Assignment]
case class Participate(val item:Participatable) extends Action[Participatable]
case class Manage(val item:Manageable) extends Action[Manageable]
case class Masquerade() extends Action[Unit]

trait Viewable
trait Manageable
trait Participatable


/// all neat and tidy... then this

object Action {
	
	val view = classOf[View]
	val submit = classOf[Submit]
	val participate = classOf[Participate]
	val manage = classOf[Manage]
	
	/**
	 * When I created the Action subclasses it worked great as you could
	 * call Manage(item) and it's all type checked. But it's less handy if
	 * you want to, say, take a string describing the action and get to
	 * a concrete Manage instance. For the moment we've got this switch
	 * statement, if it gets unmanageable then rethink things.
	 * 
	 * Note that despite all the type parameters in this method, it _still_
	 * doesn't check at compile time that the item you're passing in matches
	 * the type expected for the action type!
	 */
	def of[A<:Action[_]] (item:Any) (implicit m:ClassManifest[A]) = {
		m match {
			case m if manifest <:< manifest[View] => View(item.asInstanceOf[Viewable])
			case m if manifest <:< manifest[Submit] => Submit(item.asInstanceOf[Assignment])
			case m if manifest <:< manifest[Participate] => Participate(item.asInstanceOf[Participatable])
			case m if manifest <:< manifest[Manage] => Manage(item.asInstanceOf[Manageable])
			case m if manifest <:< manifest[Masquerade] => Masquerade()
		}
	}
}