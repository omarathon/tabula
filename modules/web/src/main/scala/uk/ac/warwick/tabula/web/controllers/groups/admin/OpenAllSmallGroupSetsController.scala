package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroupAllocationMethod, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.commands.groups.admin.OpenSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

@RequestMapping(Array("/groups/admin/department/{department}/groups/selfsignup/{action}"))
@Controller
class OpenAllSmallGroupSetsController extends GroupsController {

	@ModelAttribute("setList") def newViewModelOpen(
		@PathVariable department: Department, @PathVariable action: SmallGroupSetSelfSignUpState
	): GroupsetListViewModel = {
		new GroupsetListViewModel((user, sets) => OpenSmallGroupSetCommand(department, sets, user, action), action)
	}

	@RequestMapping
	def form(@ModelAttribute("setList") model: GroupsetListViewModel, @PathVariable department: Department, showFlash: Boolean = false) = {
		val groupSets = department.modules.asScala.flatMap(_.groupSets.asScala)
			.filter(groupset => groupset.allocationMethod == SmallGroupAllocationMethod.StudentSignUp && !groupset.deleted)
		Mav("groups/admin/groups/bulk-open", "department" -> department, "groupSets" -> groupSets, "showFlash" -> showFlash, "setState" -> model.getName)
		.crumbs(Breadcrumbs.Department(department))
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("setList") model: GroupsetListViewModel, @PathVariable department:Department) = {
		model.applyCommand(user.apparentUser)
		Redirect(Routes.admin.selfsignup(department, model.getName), "batchOpenSuccess" -> true)
	}

	class GroupsetListViewModel(val createCommand: (User, Seq[SmallGroupSet]) => Appliable[Seq[SmallGroupSet]], var action: SmallGroupSetSelfSignUpState) {
		var checkedGroupsets: JList[SmallGroupSet] = JArrayList()

		def getName = action.name

		def applyCommand(user: User)= {
			createCommand(user, checkedGroupsets.asScala).apply()
		}
	}

}
