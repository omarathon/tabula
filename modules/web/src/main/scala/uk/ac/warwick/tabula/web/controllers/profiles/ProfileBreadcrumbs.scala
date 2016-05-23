package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseYearDetails}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.web.Breadcrumbs.Active

trait ProfileBreadcrumbs {
	val Breadcrumbs = ProfileBreadcrumbs
}

object ProfileBreadcrumbs {

	/**
	 * Special case breadcrumb for a profile.
	 */
	case class Profile(profile: model.Member, isSelf: Boolean = false) extends BreadCrumb {
		val title = if (isSelf) "Your profile" else profile.fullName match {
			case None => "Profile for unknown user"
			case Some(name) => name
		}
		val url = Some(Routes.oldProfile.view(profile))
		override val tooltip = profile.fullName.getOrElse("") + " (" + profile.universityId + ")"
	}

	object Profile {

		sealed abstract class ProfileBreadcrumbIdentifier(id: String)
		case object IdentityIdentifier extends ProfileBreadcrumbIdentifier("identity")
		case object TimetableIdentifier extends ProfileBreadcrumbIdentifier("timetable")

		abstract class ProfileBreadcrumb extends BreadCrumb {
			def identifier: ProfileBreadcrumbIdentifier
			def setActive(activeIdentifier: ProfileBreadcrumbIdentifier): BreadCrumb = {
				if (this.identifier == activeIdentifier) {
					Active(this.title, this.url, this.tooltip)
				} else {
					this
				}
			}
		}

		case class Identity(member: Member) extends ProfileBreadcrumb {
			val identifier = IdentityIdentifier
			val title = member.userId
			val url = Some(Routes.Profile.identity(member))
		}

		case class Timetable(member: Member) extends ProfileBreadcrumb {
			val identifier = TimetableIdentifier
			val title = "Timetable"
			val url = Some(Routes.Profile.timetable(member))
		}

	}
}