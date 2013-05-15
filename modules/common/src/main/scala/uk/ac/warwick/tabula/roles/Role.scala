package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.CurrentUser
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import javax.persistence.Transient
import uk.ac.warwick.tabula.CaseObjectEqualityFixes

trait RoleDefinition {
	/**
	 * The canonical machine-readable name for this role. Used for listing and (for built-ins) as a database identifier
	 */
	def getName: String
	
	/**
	 * A short description of this definition; usually a human-readable version of getName
	 */
	def description: String
	
	/**
	 * Whether this role can be assigned to a user in the system. Return false for inferred roles
	 */
	def isAssignable: Boolean

	def permissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]]
	def subRoles(scope: Option[PermissionsTarget]): Set[Role]

	/**
	 * Return all permissions, resolving sub-roles
	 */
	def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]]
	
	def mayGrant(target: Permission): Boolean
}

trait BuiltInRoleDefinition extends CaseObjectEqualityFixes[BuiltInRoleDefinition] with RoleDefinition {
	final val getName = RoleDefinition.shortName(getClass.asInstanceOf[Class[_ <: BuiltInRoleDefinition]])

	private var scopedPermissions: List[Permission] = List()
	private var scopelessPermissions: List[ScopelessPermission] = List()
	private var globalPermissions: List[Permission] = List()
	private var subRoleDefinitions: Set[BuiltInRoleDefinition] = Set()

	def GrantsScopelessPermission(perms: ScopelessPermission*) =
		for (permission <- perms) scopelessPermissions ::= permission

	def GrantsScopedPermission(perms: Permission*) =
		for (permission <- perms)	scopedPermissions ::= permission

	def GrantsGlobalPermission(perms: Permission*) =
		for (permission <- perms) globalPermissions ::= permission

	def GeneratesSubRole(roles: BuiltInRoleDefinition*) =
		for (role <- roles) subRoleDefinitions += role

	def permissions(scope: Option[PermissionsTarget]) =
		ListMap() ++
		(if (scope.isDefined) scopedPermissions map { _ -> scope } else Map()) ++
		(globalPermissions map { _ -> None }) ++
		(scopelessPermissions map { _ -> None })

	def subRoles(scope: Option[PermissionsTarget]) =
		subRoleDefinitions map { defn => RoleBuilder.build(defn, scope, defn.getName) }
	
	def mayGrant(permission: Permission) =
		scopedPermissions.contains(permission) ||
		scopelessPermissions.contains(permission) ||
		globalPermissions.contains(permission) ||
		(subRoleDefinitions exists { _.mayGrant(permission) })

	/**
	 * Return all permissions, resolving sub-roles
	 */
	def allPermissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]] =
		permissions(scope) ++ (subRoleDefinitions flatMap { _.allPermissions(scope) })
		
	def isAssignable = true
}

trait UnassignableBuiltInRoleDefinition extends BuiltInRoleDefinition {
	override def isAssignable = false
}

object RoleDefinition {
	private val ObjectClassPrefix = RoleDefinition.getClass.getPackage.getName + "."

	/**
	 * Create a RoleDefinition from its name (e.g. "ModuleManagerRoleDefinition").
	 * Most likely useful in view templates, for permissions checking, or for db serialisation.
	 *
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime.
	 */
	def of(name: String): BuiltInRoleDefinition = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[BuiltInRoleDefinition]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Role definition " + name + " not recognised")
		}
	}

	def shortName(clazz: Class[_ <: BuiltInRoleDefinition])
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')
}

abstract class Role(val definition: RoleDefinition, val scope: Option[PermissionsTarget]) {

	private var permissions: Map[Permission, Option[PermissionsTarget]] = ListMap()
	private var roles: Set[Role] = Set()

	def getName = getClass.getSimpleName
	def isScoped = scope.isDefined

	lazy val explicitPermissions = permissions
	lazy val explicitPermissionsAsList = explicitPermissions.toList
	lazy val subRoles = roles

	private def grant(scope: Option[PermissionsTarget], perms: Iterable[Permission]): Unit =
		permissions ++= (perms map { _ -> scope })

	private final def applyRoleDefinition(definition: RoleDefinition): Role = {
		permissions ++= definition.permissions(scope)
		roles ++= definition.subRoles(scope)

		this
	}
	applyRoleDefinition(definition)
}

abstract class BuiltInRole(definition: BuiltInRoleDefinition, scope: Option[PermissionsTarget]) extends Role(definition, scope) {
	def this(definition: BuiltInRoleDefinition, scope: PermissionsTarget) {
		this(definition, Option(scope))
	}
}
