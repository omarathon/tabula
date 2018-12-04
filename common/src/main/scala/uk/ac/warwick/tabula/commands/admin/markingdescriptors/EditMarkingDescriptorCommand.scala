package uk.ac.warwick.tabula.commands.admin.markingdescriptors

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, DepartmentMarkingDescriptor, MarkingDescriptor}
import uk.ac.warwick.tabula.services.{AutowiringMarkingDescriptorServiceComponent, MarkingDescriptorServiceComponent}

import scala.collection.JavaConverters._

object EditMarkingDescriptorCommand {
	def apply(markingDescriptor: DepartmentMarkingDescriptor): Appliable[MarkingDescriptor] with ModifyMarkingDescriptorState =
		new EditMarkingDescriptorCommandInternal(markingDescriptor.department, markingDescriptor)
			with ComposableCommand[MarkingDescriptor]
			with EditMarkingDescriptorValidation
			with EditMarkingDescriptorState
			with ModifyMarkingDescriptorPermissions
			with AddMarkingDescriptorDescription
			with AutowiringMarkingDescriptorServiceComponent
}

trait EditMarkingDescriptorState extends ModifyMarkingDescriptorState {
	def markingDescriptor: DepartmentMarkingDescriptor
}

class EditMarkingDescriptorCommandInternal(val department: Department, val markingDescriptor: DepartmentMarkingDescriptor) extends CommandInternal[MarkingDescriptor] {
	self: EditMarkingDescriptorState with MarkingDescriptorServiceComponent =>

	text = markingDescriptor.text
	markPoints = markingDescriptor.markPoints.asJava

	override def applyInternal(): MarkingDescriptor = {
		markingDescriptor.minMarkPoint = markPoints.asScala.min
		markingDescriptor.maxMarkPoint = markPoints.asScala.max
		markingDescriptor.text = text

		markingDescriptorService.save(markingDescriptor)

		markingDescriptor
	}
}

trait EditMarkingDescriptorValidation extends ModifyMarkingDescriptorValidation {
	self: EditMarkingDescriptorState with MarkingDescriptorServiceComponent =>
	override def markPointsAlreadyExist: Boolean = markPoints.asScala.exists(mp => markingDescriptorService.getDepartmentMarkingDescriptors(department).filterNot(_ == markingDescriptor).exists(_.isForMarkPoint(mp)))
}

trait EditMarkingDescriptorCommandDescription extends Describable[MarkingDescriptor] {
	self: EditMarkingDescriptorState =>

	override def describe(d: Description): Unit = {
		d.markingDescriptor(markingDescriptor)
	}
}
