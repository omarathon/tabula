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
	case class Profile(val profile: model.Member) extends Abstract {
		val title = profile.universityId
		val url = Routes.profile.view(profile)
		override val tooltip = profile.fullName
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