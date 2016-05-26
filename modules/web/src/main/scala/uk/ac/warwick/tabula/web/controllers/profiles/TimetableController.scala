package uk.ac.warwick.tabula.web.controllers.profiles

import javax.servlet.http.HttpServletRequest

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.view.RedirectView
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/profiles/timetable"))
class ViewMyTimetableController extends ProfilesController {
	@RequestMapping def redirect(user: CurrentUser) =
		user.profile match {
			case Some(profile) => Redirect(Routes.profiles.oldProfile.viewTimetable(profile))
			case _ => Redirect(Routes.profiles.home)
		}
}

@Controller
@RequestMapping(value = Array("/profiles/timetable/{member}"))
class TimetableForMemberController extends ProfilesController
	with AutowiringUserLookupComponent
	with AutowiringProfileServiceComponent {

	@RequestMapping
	def viewTimetable(
		@PathVariable member: Member,
		@RequestParam(value = "now", required = false) now: JLong
	) = {
		val renderDate = if (now == null) DateTime.now else new DateTime(now)
		val isSelf = member.universityId == user.universityId

		Mav("profiles/profile/view_timetable",
			"profile" -> member,
			"isSelf" -> isSelf,
			"renderDate" -> renderDate.toDate
		).crumbs(Breadcrumbs.Profile(member, isSelf))
	}

}

/*** LEGACY REDIRECTS BELOW ***/

@Controller
@RequestMapping(value = Array("/profiles/timetable/api"))
class TimetableController extends BaseController {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@RequestMapping
	def redirect(@RequestParam(value="whoFor") whoFor: Member, request: HttpServletRequest) = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendar(whoFor) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

}

@Controller
@RequestMapping(value = Array("/profiles/timetable/ical/{timetableHash}.ics"))
class TimetableICalController extends BaseController {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@RequestMapping
	def redirect(@PathVariable timetableHash: String, request: HttpServletRequest) = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICalForHash(mandatory(timetableHash)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

}

@Controller
@RequestMapping(value = Array("/profiles/timetable/ical"))
class LegacyTimetableICalController extends BaseController {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@RequestMapping
	def redirect(@RequestParam("timetableHash") timetableHash: String, request: HttpServletRequest) = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICalForHash(mandatory(timetableHash)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

}

@Controller
@RequestMapping(value = Array("/profiles/timetable/{member}/ical", "/profiles/timetable/{member}/timetable.ics"))
class TimetableICalForMemberController extends BaseController {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@RequestMapping
	def redirect(@PathVariable member: Member, request: HttpServletRequest) = {
		val r = new RedirectView(toplevelUrl + Routes.api.timetables.calendarICal(mandatory(member)) + request.getQueryString.maybeText.map("?" + _).getOrElse(""))
		r.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
		r
	}

}
