package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.hibernate.validator.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.groups.commands.admin._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import org.springframework.validation.BeanPropertyBindingResult
import uk.ac.warwick.tabula.commands.{UpstreamGroupPropertyEditor, UpstreamGroup, Appliable}
import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User

trait SmallGroupSetsController extends GroupsController {
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats = SmallGroupFormat.members
	
	@ModelAttribute("allDays") def allDays = DayOfWeek.members
		
	@ModelAttribute("module") def module(@PathVariable("module") module: Module) = module 
	
	def allTermWeekRanges(cmd: ModifySmallGroupSetCommand) = {
		WeekRange.termWeekRanges(Option(cmd.academicYear).getOrElse(AcademicYear.guessByDate(DateTime.now)))
		.map { TermWeekRange(_) }
	}
	
	override final def binding[A](binder: WebDataBinder, cmd: A) {		
		binder.registerCustomEditor(classOf[SmallGroupFormat], new AbstractPropertyEditor[SmallGroupFormat] {
			override def fromString(code: String) = SmallGroupFormat.fromCode(code)			
			override def toString(format: SmallGroupFormat) = format.code
		})
		binder.registerCustomEditor(classOf[SmallGroupAllocationMethod], new AbstractPropertyEditor[SmallGroupAllocationMethod] {
			override def fromString(code: String) = SmallGroupAllocationMethod.fromDatabase(code)			
			override def toString(method: SmallGroupAllocationMethod) = method.dbValue
		})
	}
	
}

case class TermWeekRange(val weekRange: WeekRange) {
	def isFull(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.forall(weeks.contains(_))
	def isPartial(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.exists(weeks.contains(_))
}

@RequestMapping(Array("/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[CreateSmallGroupSetCommand]
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module) = 
		new CreateSmallGroupSetCommand(module)
		
	@RequestMapping
	def form(cmd: CreateSmallGroupSetCommand) = {
		cmd.afterBind()

		Mav("admin/groups/new",
			"allTermWeekRanges" -> allTermWeekRanges(cmd),
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups
		).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		cmd.afterBind()

		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			
			// Redirect straight to allocation
			Redirect(Routes.admin.allocate(set))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/edit"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[EditSmallGroupSetCommand]
		
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable("set") set: SmallGroupSet, user:CurrentUser) =
		new EditSmallGroupSetCommand(set, user.apparentUser)

	@ModelAttribute("canDelete") def canDelete(@PathVariable("set") set: SmallGroupSet) = {
		val cmd = new DeleteSmallGroupSetCommand(set.module, set)
		val errors = new BeanPropertyBindingResult(cmd, "cmd")
		cmd.validateCanDelete(errors)
		!errors.hasErrors
	}
	
	@RequestMapping
	def form(cmd: EditSmallGroupSetCommand) = {
		cmd.afterBind()

		Mav("admin/groups/edit",
			"allTermWeekRanges" -> allTermWeekRanges(cmd),
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups
		).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def update(@Valid cmd: EditSmallGroupSetCommand, errors: Errors) = {
		cmd.afterBind()

		if (!errors.hasErrors) {
			cmd.apply()
		}

		form(cmd)
	}

	@RequestMapping(method=Array(POST), params=Array("action!=refresh", "action!=update"))
	def submit(@Valid cmd: EditSmallGroupSetCommand, errors: Errors) = {
		cmd.afterBind()

		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/delete"))
@Controller
class DeleteSmallGroupSetController extends GroupsController {
	
	validatesSelf[DeleteSmallGroupSetCommand]
	
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("deleteSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("set") set: SmallGroupSet) = 
		new DeleteSmallGroupSetCommand(module, set)

	@RequestMapping
	def form(cmd: DeleteSmallGroupSetCommand) =
		Mav("admin/groups/delete")
		.crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: DeleteSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/archive"))
@Controller
class ArchiveSmallGroupSetController extends GroupsController {
		
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("archiveSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("set") set: SmallGroupSet) = 
		new ArchiveSmallGroupSetCommand(module, set)

	@RequestMapping
	def form(cmd: ArchiveSmallGroupSetCommand) =
		Mav("admin/groups/archive").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveSmallGroupSetCommand) = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
	
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/release"))
@Controller
class ReleaseSmallGroupSetController extends GroupsController {

  @ModelAttribute("releaseGroupSetCommand") def getReleaseGroupSetCommand(@PathVariable("set") set:SmallGroupSet):Appliable[Seq[SmallGroupSet]]={
    new ReleaseGroupSetCommandImpl( Seq(set),user.apparentUser )
  }

  @RequestMapping
  def form(@ModelAttribute("releaseGroupSetCommand") cmd: Appliable[SmallGroupSet]) =
    Mav("admin/groups/release").noLayoutIf(ajax)

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("releaseGroupSetCommand") cmd: Appliable[SmallGroupSet]) = {
    cmd.apply()
    Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
  }
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/open"))
@Controller
class OpenSmallGroupSetController extends GroupsController {

	@ModelAttribute("openGroupSetCommand")
	def getOpenGroupSetCommand(@PathVariable("set") set: SmallGroupSet): Appliable[Seq[SmallGroupSet]] with OpenSmallGroupSetState = {
		OpenSmallGroupSetCommand(Seq(set), user.apparentUser)
	}

	@RequestMapping
	def form(@ModelAttribute("openGroupSetCommand") cmd: Appliable[Seq[SmallGroupSet]]) =
		Mav("admin/groups/open").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("openGroupSetCommand") cmd: Appliable[Seq[SmallGroupSet]]) = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
}


@RequestMapping(Array("/admin/department/{department}/groups/release"))
@Controller
class ReleaseAllSmallGroupSetsController extends GroupsController {

  @ModelAttribute("moduleList") def newViewModel():ModuleListViewModel={
    new ModuleListViewModel()
  }

  @RequestMapping
  def form(@ModelAttribute("moduleList") model: ModuleListViewModel, @PathVariable department:Department, showFlash:Boolean=false) ={
    Mav("admin/groups/bulk-release", "department"->department, "modules"->department.modules, "showFlash"->showFlash)
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("moduleList") model: ModuleListViewModel,@PathVariable department:Department) = {
    model.createCommand(user.apparentUser).apply()
    Redirect("/admin/department/%s/groups/release".format(department.code), "batchReleaseSuccess"->true)
  }

  class ModuleListViewModel(){
    var checkedModules:JList[Module] = JArrayList()
    var notifyStudents:JBoolean = true
    var notifyTutors:JBoolean = true

    def smallGroupSets() = {
      checkedModules.asScala.flatMap(mod=>
        mod.groupSets.asScala
      )
    }

    def createCommand(user:User):Appliable[Seq[SmallGroupSet]] = {
      new ReleaseGroupSetCommandImpl(smallGroupSets(), user)
    }
  }

}

@RequestMapping(Array("/admin/department/{department}/groups/open"))
@Controller
class OpenAllSmallGroupSetsController extends GroupsController {

	@ModelAttribute("setList") def newViewModel():GroupsetListViewModel={
		new GroupsetListViewModel((user, sets)=>OpenSmallGroupSetCommand(sets, user))
	}

	@RequestMapping
	def form(@ModelAttribute("setList") model: GroupsetListViewModel, @PathVariable department:Department, showFlash:Boolean=false) ={
		val groupSets = department.modules.asScala.flatMap(_.groupSets.asScala).filter(_.allocationMethod == SmallGroupAllocationMethod.StudentSignUp)
		Mav("admin/groups/bulk-open", "department"->department, "groupSets"->groupSets, "showFlash"->showFlash)
	}


	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("setList") model: GroupsetListViewModel,@PathVariable department:Department) = {
		model.applyCommand(user.apparentUser)
		Redirect("/admin/department/%s/groups/open".format(department.code), "batchOpenSuccess"->true)
	}

	class GroupsetListViewModel(val createCommand: (User, Seq[SmallGroupSet])=>Appliable[Seq[SmallGroupSet]]){
		var checkedGroupsets:JList[SmallGroupSet] = JArrayList()

		def applyCommand(user:User)= {
			createCommand(user, checkedGroupsets.asScala).apply()
		}
	}

}

