package uk.ac.warwick.tabula.admin.web.controllers.permissions

import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import org.springframework.web.servlet.mvc.Controller
import uk.ac.warwick.tabula.roles.{SelectorBuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.permissions.{SelectorPermission, Permissions, PermissionsSelector, Permission}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.permissions.PermissionsService

@Controller @RequestMapping(Array("/roles", "/department/{department}/roles"))
class RoleTableController extends AdminController {

	var permissionsService = Wire[PermissionsService]

	type Response = Option[Boolean]
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Partial: Response = None // allowed under certain circumstances

	type RolesTable = Seq[(Permission, Seq[(RoleDefinition, Response)])]

	private def parentDepartments(department: Department): Seq[Department] =
		if (department == null) Nil
		else if (department.hasParent) department +: parentDepartments(department.parent)
		else Seq(department)

	@ModelAttribute("rolesTable") def rolesTable(@PathVariable department: Department): RolesTable = {
		val builtInRoleDefinitions = ReflectionHelper.allBuiltInRoleDefinitions

		val allDepartments = parentDepartments(department)

		val relationshipTypes =
			allDepartments
				.flatMap { _.displayedStudentRelationshipTypes }
				.distinct

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

		val allDefinitions = (builtInRoleDefinitions ++ selectorBuiltInRoleDefinitions ++ customRoleDefinitions)

		def groupFn(p: Permission) = {
			val simpleName = Permissions.shortName(p.getClass)

			val parentName =
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))

			parentName
		}

		ReflectionHelper.allPermissions
			.filter { p => groupFn(p).hasText }
			.sortBy { p => groupFn(p) }
			.flatMap { p =>
				if (p.isInstanceOf[SelectorPermission[_]]) {
					p +: relationshipTypes.map { relationshipType =>
						SelectorPermission.of(p.getName, relationshipType)
					}
				} else {
					Seq(p)
				}
			}
			.map { permission =>
				(permission, allDefinitions.map { definition =>
					(definition, Some(definition.mayGrant(permission)))
				})
			}
	}

	@RequestMapping def page = "admin/permissions/role_table"

}
