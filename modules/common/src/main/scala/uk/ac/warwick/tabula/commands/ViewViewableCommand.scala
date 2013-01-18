package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.actions.Viewable
import uk.ac.warwick.tabula.actions.View

class ViewViewableCommand[V <: Viewable : Manifest](val value: V) extends Command[V] with ReadOnly with Unaudited {
	PermissionsCheck(View(mandatory(value)))
	override def applyInternal() = value
}