package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.admin.markingdescriptors.{AddMarkingDescriptorCommand, DeleteMarkingDescriptorCommand, EditMarkingDescriptorCommand, ModifyMarkingDescriptorState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringMarkingDescriptorServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.admin.AdminController

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(value = Array("/admin/department/{department}/markingdescriptors"))
class MarkingDescriptorsController extends AdminController with AutowiringMarkingDescriptorServiceComponent {
	@ModelAttribute("markPoints")
	def markPoints: Seq[MarkPoint] = MarkPoint.all


	@ModelAttribute("universityDescriptors")
	def universityDescriptors: JMap[MarkPoint, UniversityMarkingDescriptor] = {
		val descriptors = markingDescriptorService.getMarkingDescriptors

		(for {
			markPoint <- MarkPoint.all
			descriptor <- descriptors.find(_.isForMarkPoint(markPoint))
		} yield markPoint -> descriptor).toMap.asJava
	}

	@ModelAttribute("departmentDescriptors")
	def departmentDescriptors(@PathVariable department: Department): JMap[MarkPoint, DepartmentMarkingDescriptor] = {
		val descriptors = markingDescriptorService.getDepartmentMarkingDescriptors(department)

		(for {
			markPoint <- MarkPoint.all
			descriptor <- descriptors.find(_.isForMarkPoint(markPoint))
		} yield markPoint -> descriptor).toMap.asJava
	}

	@GetMapping
	def index(@PathVariable department: Department): Mav = Mav("admin/markingdescriptors/index")
}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/markingdescriptors/new"))
class AddMarkingDescriptorController extends AdminController {
	validatesSelf[SelfValidating]

	type CommandType = Appliable[MarkingDescriptor] with ModifyMarkingDescriptorState

	@ModelAttribute("command")
	def command(@PathVariable department: Department): CommandType =
		AddMarkingDescriptorCommand.apply(mandatory(department))

	@ModelAttribute("markPoints")
	def markPoints: Seq[MarkPoint] = MarkPoint.all

	def render: Mav = Mav("admin/markingdescriptors/new")

	@GetMapping
	def form(@PathVariable department: Department, @ModelAttribute("command") command: CommandType): Mav = {
		render
	}

	@PostMapping
	def process(@PathVariable department: Department, @Valid @ModelAttribute("command") command: CommandType, bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			render
		} else {
			command.apply()

			Redirect(s"/admin/department/${department.code}/markingdescriptors")
		}
	}
}

@Controller
@RequestMapping(path = Array("/admin/department/{department}/markingdescriptors/{markingDescriptor}/edit"))
class EditMarkingDescriptorController extends AdminController {
	validatesSelf[SelfValidating]

	type CommandType = Appliable[MarkingDescriptor] with ModifyMarkingDescriptorState

	@ModelAttribute("command")
	def command(@PathVariable("markingDescriptor") markingDescriptor: DepartmentMarkingDescriptor): CommandType =
		EditMarkingDescriptorCommand.apply(mandatory(markingDescriptor))

	@ModelAttribute("markPoints")
	def markPoints: Seq[MarkPoint] = MarkPoint.all

	def render: Mav = Mav("admin/markingdescriptors/edit")

	@GetMapping
	def form(@PathVariable department: Department): Mav = render

	@PostMapping
	def process(@PathVariable department: Department, @Valid @ModelAttribute("command") command: CommandType, bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			render
		} else {
			command.apply()

			Redirect(s"/admin/department/${department.code}/markingdescriptors")
		}
	}
}

@Controller
@RequestMapping(path = Array("/admin/department/{department}/markingdescriptors/{markingDescriptor}/delete"))
class DeleteMarkingDescriptorController extends AdminController {
	validatesSelf[SelfValidating]

	type CommandType = Appliable[Unit]

	@ModelAttribute("command")
	def command(@PathVariable("markingDescriptor") markingDescriptor: DepartmentMarkingDescriptor): CommandType =
		DeleteMarkingDescriptorCommand.apply(mandatory(markingDescriptor))

	def render: Mav = Mav("admin/markingdescriptors/delete")

	@GetMapping
	def form(@PathVariable department: Department): Mav = render

	@PostMapping
	def process(@PathVariable department: Department, @Valid @ModelAttribute("command") command: CommandType, bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			render
		} else {
			command.apply()

			Redirect(s"/admin/department/${department.code}/markingdescriptors")
		}
	}
}