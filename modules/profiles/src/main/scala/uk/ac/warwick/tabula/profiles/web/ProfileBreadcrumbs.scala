package uk.ac.warwick.tabula.profiles.web

import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.data.model

trait ProfileBreadcrumbs {
	val Breadcrumbs = ProfileBreadcrumbs
}

object ProfileBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: String, override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for a profile.
	 */
	case class Profile(val profile: model.Member, val isSelf: Boolean = false) extends Abstract {
		val title = if (isSelf) "Your profile" else profile.fullName match {
			case None => "Profile for unknown user"
			case Some(name) => name
		}
		val url = Routes.profile.view(profile)
		override val tooltip = profile.fullName.getOrElse("") + " (" + profile.universityId + ")"
	}

	/**
	 * Not current used: a breadcrumb without a link, to represent
	 * the current page. We don't currently include the current page in crumbs.
	 */
	case class Current(val title: String) extends Abstract {
		val url = null
		override def linked = false
	}
}