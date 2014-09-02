package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.groups.commands.admin.{ReleaseGroupSetCommandImpl, ReleasedSmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

@RequestMapping(Array("/admin/department/{department}/groups/release"))
@Controller
class ReleaseAllSmallGroupSetsController extends GroupsController {

  @ModelAttribute("moduleList") def newViewModel():ModuleListViewModel={
    new ModuleListViewModel()
  }

  @RequestMapping
  def form(@ModelAttribute("moduleList") model: ModuleListViewModel, @PathVariable department:Department, showFlash:Boolean=false) ={
    Mav("admin/groups/bulk-release", "department"->department, "modules"->department.modules, "showFlash"->showFlash)
    .crumbs(Breadcrumbs.Department(department))
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("moduleList") model: ModuleListViewModel, @PathVariable department:Department) = {
    model.createCommand(user.apparentUser).apply()
    Redirect(Routes.admin.release(department), "batchReleaseSuccess"->true)
  }

	class ModuleListViewModel() {
		var checkedModules: JList[Module] = JArrayList()
		var notifyStudents: JBoolean = true
		var notifyTutors: JBoolean = true
		var sendEmail: JBoolean = true

		def smallGroupSets() = {
			if (checkedModules == null) {
				// if  no modules are selected, spring binds null, not an empty list :-(
				Nil
			} else {
				checkedModules.asScala.flatMap(mod =>
					mod.groupSets.asScala
				)
			}
		}

		def createCommand(user: User): Appliable[Seq[ReleasedSmallGroupSet]] = {
			val command = new ReleaseGroupSetCommandImpl(smallGroupSets(), user)
			command.notifyStudents = notifyStudents
			command.notifyTutors = notifyTutors
			command.sendEmail = sendEmail
			command
		}
	}
}
