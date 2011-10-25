package uk.ac.warwick.courses
import uk.ac.warwick.courses.data.model.Assignment

package actions {

abstract class Action

case class View(obj:Any) extends Action
case class Submit(a:Assignment) extends Action
case class Manage(obj:Any) extends Action

}