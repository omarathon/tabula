package uk.ac.warwick.courses.actions
import uk.ac.warwick.courses.data.model.Assignment

abstract class Action

/**
 * Marker trait for classes that can be viewed
 */
trait Viewable
trait Manageable
trait Participatable

// View 
case class View(obj:Viewable) extends Action
case class Submit(a:Assignment) extends Action
case class Participate(a:Participatable) extends Action
case class Manage(obj:Manageable) extends Action

