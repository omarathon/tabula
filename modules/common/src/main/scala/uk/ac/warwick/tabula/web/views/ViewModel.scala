package uk.ac.warwick.tabula.web.views

/**
 * Objects to pass to views for rendering.
 */
object ViewModel {

	abstract class MenuEntry(val kind: String)

	case class MenuSeparator() extends MenuEntry("separator")

	case class MenuAction(
			name: String,
			icon: String,
			enabled: Boolean = true,
			disabledPopupText: Option[String] = None) extends MenuEntry("action") {
		def disabledIf(cond: => Boolean, disabledMessage: String) = copy(enabled=(!cond), disabledPopupText=Some(disabledMessage))
	}

}
