package uk.ac.warwick.tabula.admin.web.controllers.permissions

import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.roles.{SelectorBuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.permissions.{SelectorPermission, Permissions, PermissionsSelector, Permission}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition

@Controller
class RoleTableController extends AdminController {

	var permissionsService = Wire[PermissionsService]
	var relationshipService = Wire[RelationshipService]

	type Response = Option[Boolean]
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Partial: Response = None // allowed under certain circumstances

	type RolesTable = Seq[(Permission, Seq[(RoleDefinition, Response)])]

	private def parentDepartments(department: Department): Seq[Department] =
		if (mandatory(department).hasParent) department +: parentDepartments(department.parent)
		else Seq(department)

	private def baseName(roleDefinition: RoleDefinition): String = roleDefinition match {
		case custom: CustomRoleDefinition => baseName(custom.baseRoleDefinition)
		case _ => roleDefinition.getName.substring(0, 5)
	}

	private def rolesTable(department: Option[Department]): RolesTable = {
		val builtInRoleDefinitions = ReflectionHelper.allBuiltInRoleDefinitions

		val allDepartments = department.toSeq.flatMap(parentDepartments)

		val relationshipTypes =
			if (allDepartments.isEmpty) relationshipService.allStudentRelationshipTypes.filter { _.defaultDisplay }
			else allDepartments.flatMap { _.displayedStudentRelationshipTypes }.distinct

		val selectorBuiltInRoleDefinitions =
			ReflectionHelper.allSelectorBuiltInRoleDefinitionNames.flatMap { name =>
				SelectorBuiltInRoleDefinition.of(name, PermissionsSelector.Any[StudentRelationshipType]) +:
					relationshipTypes.map { relationshipType =>
						SelectorBuiltInRoleDefinition.of(name, relationshipType)
					}
			}

		val customRoleDefinitions =
			allDepartments
				.flatMap { department => permissionsService.getCustomRoleDefinitionsFor(department) }
				.filterNot { _.replacesBaseDefinition }

		val allDefinitionsWithoutReplacements =
			(builtInRoleDefinitions ++ selectorBuiltInRoleDefinitions ++ customRoleDefinitions)
				.filter { _.isAssignable }
				.sortBy { _.allPermissions(Some(null)).size }
				.groupBy(baseName)
				.toSeq.sortBy { case (substr, defs) =>
					// Show selector ones first
					val selectorSort = defs.head match {
						case _: SelectorBuiltInRoleDefinition[_] => 0
						case _ => 1
					}

					(selectorSort, defs.map { _.allPermissions(Some(null)).size }.max, substr)
				}
				.flatMap { case (_, defs) => defs }

		val allDefinitions =
			allDefinitionsWithoutReplacements.map { definition =>
				(definition, allDepartments.flatMap { _.replacedRoleDefinitionFor(definition) }.headOption.getOrElse(definition))
			}

		def groupFn(p: Permission) = {
			val simpleName = Permissions.shortName(p.getClass)

			val parentName =
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))

			parentName
		}

		ReflectionHelper.allPermissions
			.filter { p => groupFn(p).hasText }
			.map { permission =>
				(permission, allDefinitions.map {
					case (actualDefinition, permsDefinition) => (actualDefinition, permsDefinition match {
						case definition: SelectorBuiltInRoleDefinition[StudentRelationshipType@unchecked] => {
							permission match {
								case _ if definition.mayGrant(permission) => Some(true)
								case selectorPermission: SelectorPermission[StudentRelationshipType@unchecked]
									if definition.allPermissions(Some(null)).exists { case (p, _) => p.getName == selectorPermission.getName} &&
										definition.selector <= selectorPermission.selector => None
								case _ => Some(false)
							}
						}
						case definition => Some(definition.mayGrant(permission))
					})
				})
			}
			.filter { case (p, defs) => defs.exists { case (_, result) => !result.isDefined || result.exists { b => b } } }
			.sortBy { case (p, _) => groupFn(p) }
	}

	@RequestMapping(Array("/roles")) def generic =
		Mav("admin/permissions/role_table", "rolesTable" -> rolesTable(None))

	@RequestMapping(Array("/department/{department}/roles")) def forDepartment(@PathVariable department: Department) =
		Mav("admin/permissions/role_table", "rolesTable" -> rolesTable(Some(department)))

}
