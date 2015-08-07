package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.{ReleaseGroupSetCommandImpl, ReleaseSmallGroupSetCommand}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModule, ViewSet}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import scala.collection.JavaConverters._

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/release"))
@Controller
class ReleaseSmallGroupSetController extends GroupsController {

	@ModelAttribute("releaseGroupSetCommand") def getReleaseGroupSetCommand(@PathVariable("set") set: SmallGroupSet): ReleaseSmallGroupSetCommand = {
		new ReleaseGroupSetCommandImpl(Seq(set), user.apparentUser)
	}

	@RequestMapping
	def form(@ModelAttribute("releaseGroupSetCommand") cmd: ReleaseSmallGroupSetCommand) =
		Mav("groups/admin/groups/release").noLayoutIf(ajax)


	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("releaseGroupSetCommand") cmd: ReleaseSmallGroupSetCommand) = {
		val updatedSet = cmd.apply() match {
			case releasedSet :: Nil => releasedSet.set
			case _ => throw new IllegalStateException("Received multiple updated sets from a single update operation!")
		}
		val groupSetItem = new ViewSet(updatedSet, updatedSet.groups.asScala.sorted, GroupsViewModel.Tutor)
		val moduleItem = new ViewModule(updatedSet.module, Seq(groupSetItem), true)
		Mav("groups/admin/groups/single_groupset",
			"groupsetItem" -> groupSetItem,
			"moduleItem" -> moduleItem,
			"notificationSentMessage" -> cmd.describeOutcome).noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
}
