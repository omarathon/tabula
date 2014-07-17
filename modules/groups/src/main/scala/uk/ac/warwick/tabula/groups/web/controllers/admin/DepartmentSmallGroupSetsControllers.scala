package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.groups.commands.admin._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}

trait DepartmentSmallGroupSetsController extends GroupsController {

	validatesSelf[SelfValidating]
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats = SmallGroupFormat.members
	
	@ModelAttribute("allDays") def allDays = DayOfWeek.members

	case class TermWeekRange(val weekRange: WeekRange) {
		def isFull(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.forall(weeks.contains(_))
		def isPartial(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.exists(weeks.contains(_))
	}
	
	def allTermWeekRanges(academicYear: Option[AcademicYear]) = {
		WeekRange.termWeekRanges(academicYear.getOrElse(AcademicYear.guessByDate(DateTime.now))).map { TermWeekRange(_) }
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

@RequestMapping(Array("/admin/department/{department}/groups/new"))
@Controller
class CreateDepartmentSmallGroupSetController extends DepartmentSmallGroupSetsController {

	type CreateDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with CreateDepartmentSmallGroupSetCommandState

	@ModelAttribute("createDepartmentSmallGroupSetCommand") def cmd(@PathVariable("department") department: Department): CreateDepartmentSmallGroupSetCommand =
		ModifyDepartmentSmallGroupSetCommand.create(department)
		
	@RequestMapping
	def form(cmd: CreateDepartmentSmallGroupSetCommand) = {
		Mav("admin/groups/cross-module/new",
			"allTermWeekRanges" -> allTermWeekRanges(Option(cmd.academicYear))
		).crumbs(Breadcrumbs.Department(cmd.department))
	}
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: CreateDepartmentSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			
			// Redirect straight to allocation only for manual allocation groups 
			/* TODO FIXME if (set.allocationMethod == SmallGroupAllocationMethod.Manual) Redirect(Routes.admin.allocate(set))
			else */ Redirect(Routes.admin(cmd.department))
		}
	}
}

@RequestMapping(Array("/admin/department/{department}/groups/{smallGroupSet}/edit"))
@Controller
class EditDepartmentSmallGroupSetController extends DepartmentSmallGroupSetsController {

	type EditDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with EditDepartmentSmallGroupSetCommandState

	@ModelAttribute("editDepartmentSmallGroupSetCommand") def cmd(@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupSetCommand =
		ModifyDepartmentSmallGroupSetCommand.edit(set)

	@RequestMapping
	def form(cmd: EditDepartmentSmallGroupSetCommand, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/cross-module/edit",
			"allTermWeekRanges" -> allTermWeekRanges(Option(cmd.academicYear))
		).crumbs(Breadcrumbs.Department(set.department))
	}

	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: EditDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		if (errors.hasErrors) form(cmd, set)
		else {
			cmd.apply()
			Redirect(Routes.admin(set.department))
		}
	}
}
