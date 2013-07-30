package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.commands.{ListStudentsGroupsCommandImpl}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.data.model.Member

/**
 * Displays the groups that the current user is a member of.
 */
@Controller
@RequestMapping(Array("/student/{member}"))
class StudentGroupsController extends GroupsController {

	@ModelAttribute("command") def command(@PathVariable member:Member) =
		new ListStudentsGroupsCommandImpl(member.asSsoUser)

	@RequestMapping(method = Array(POST, GET))
	def listGroups(@ModelAttribute("command") command: ListStudentsGroupsCommandImpl): Mav = {
		val mapping = command.apply()

		val data = generateGroupsViewModel(mapping)

		val title = {
			val smallGroups: Seq[SmallGroupSet] = for (
				setToGroupsMap <- mapping.values.toSeq;
				groupSet <- setToGroupsMap.keys
			) yield groupSet

			val formats = smallGroups.map(_.format.description).distinct
			val pluralisedFormats = formats.map {
				case s:String if (s == Example.description) => s + "es"
				case s:String => s + "s"
				case _ =>
			}
			pluralisedFormats.mkString(", ")
		}

		Mav("groups/students_groups",
			"title" -> title,
			"data" -> data
		).noLayoutIf(ajax)
	}

}
