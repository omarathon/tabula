package uk.ac.warwick.tabula.home.web.controllers

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.ActivityService._
import uk.ac.warwick.tabula.commands.Appliable
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute}
import uk.ac.warwick.tabula.home.commands.ActivityStreamCommand
import uk.ac.warwick.tabula.services.{SearchPagination, ActivityStreamRequest}

@Controller
class ActivityStreamController extends BaseController with ActivityJsonMav {

	@ModelAttribute("command")
	def command(
			user: CurrentUser,
			@RequestParam(defaultValue="20") max: Int,
			@RequestParam types: JList[String],
			@RequestParam(defaultValue="0") minPriority: Double, // minPriority of zero means we show all by default
			@RequestParam(defaultValue="false") includeDismissed: Boolean,
			@RequestParam(required=false) lastDoc: JInteger,
			@RequestParam(required=false) last: JLong,
			@RequestParam(required=false) token: JLong) = {
		val typeSet = if (types.isEmpty) None else Some(types.asScala.toSet)
		val pagination = if (token != null && lastDoc != null && last != null) {
			Some(SearchPagination(lastDoc, last, token))
		} else {
			None
		}
		val request = ActivityStreamRequest(user.apparentUser, max, minPriority, includeDismissed, typeSet, pagination)
		ActivityStreamCommand(request)
	}

	@RequestMapping(value=Array("/activity/@me"))
	def userStream(@ModelAttribute("command") command: Appliable[PagedActivities]) = {
		val activities = command.apply()
		toMav(activities.items)
	}
}
