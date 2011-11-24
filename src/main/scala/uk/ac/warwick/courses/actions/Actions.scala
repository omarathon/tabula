package uk.ac.warwick.courses.actions
import uk.ac.warwick.courses.data.model.Assignment

trait Viewable
trait Manageable

abstract class Action

case class View(obj:Viewable) extends Action
case class Submit(a:Assignment) extends Action
case class Manage(obj:Manageable) extends Action

package A {
	class Beef
}