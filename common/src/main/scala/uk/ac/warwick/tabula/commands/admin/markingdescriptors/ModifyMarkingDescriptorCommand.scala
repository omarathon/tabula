package uk.ac.warwick.tabula.commands.admin.markingdescriptors

import org.springframework.validation.{Errors, ValidationUtils}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.{Department, MarkPoint}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.MarkingDescriptorServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

trait ModifyMarkingDescriptorValidation extends SelfValidating {
	self: ModifyMarkingDescriptorState with MarkingDescriptorServiceComponent =>

	override def validate(errors: Errors): Unit = {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "text", "NotEmpty")

		if (markPoints.isEmpty) {
			errors.rejectValue("markPoints", "markingDescriptor.markPoints.empty")
		}

		if (!markPointsAreContiguous) {
			errors.rejectValue("markPoints", "markingDescriptor.markPoints.nonContiguous")
		}

		if (markPointsAlreadyExist) {
			errors.rejectValue("markPoints", "markingDescriptor.markPoints.exists")
		}
	}

	private def markPointsAreContiguous: Boolean = {
		val sortedMarkPoints = markPoints.asScala.sorted

		sortedMarkPoints.zipWithIndex.drop(1).forall { case (markPoint, index) =>
			markPoint.previous.contains(sortedMarkPoints(index - 1))
		}
	}

	def markPointsAlreadyExist: Boolean
}

trait ModifyMarkingDescriptorState {
	def department: Department

	var markPoints: JList[MarkPoint] = _
	var text: String = _
}

trait ModifyMarkingDescriptorPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyMarkingDescriptorState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Department.Manage, mandatory(department))
	}
}

