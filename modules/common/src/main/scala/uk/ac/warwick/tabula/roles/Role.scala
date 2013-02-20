package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.CurrentUser
import scala.annotation.tailrec
import scala.reflect.BeanProperty
import scala.collection.immutable.ListMap
import javax.persistence.Transient

trait RoleDefinition {
	def getName: String
	
	def permissions(scope: Option[PermissionsTarget]): Map[Permission, Option[PermissionsTarget]]
	def subRoles(scope: Option[PermissionsTarget]): Set[Role]
}

trait BuiltInRoleDefinition extends RoleDefinition {
	final val getName = RoleDefinition.shortName(getClass.asInstanceOf[Class[_ <: BuiltInRoleDefinition]])
	
	private var scopedPermissions: List[Permission] = List()
	private var scopelessPermissions: List[ScopelessPermission] = List()
	private var globalPermissions: List[Permission] = List()
	private var subRoleDefinitions: Set[RoleDefinition] = Set()
	
	def GrantsScopelessPermission(perms: ScopelessPermission*) = 
		for (permission <- perms) scopelessPermissions ::= permission
			
	def GrantsScopedPermission(perms: Permission*) =
		for (permission <- perms)	scopedPermissions ::= permission
			
	def GrantsGlobalPermission(perms: Permission*) =
		for (permission <- perms) globalPermissions ::= permission
		
	def GeneratesSubRole(roles: RoleDefinition*) =
		for (role <- roles) subRoleDefinitions += role
		
	def permissions(scope: Option[PermissionsTarget]) = 
		ListMap() ++
		(if (scope.isDefined) scopedPermissions map { _ -> scope } else Map()) ++
		(globalPermissions map { _ -> None }) ++
		(scopelessPermissions map { _ -> None })
		
	def subRoles(scope: Option[PermissionsTarget]) =
		subRoleDefinitions map { RoleBuilder.build(_, scope) }
	
	/* We need to override equals() here because under heavy load, the class loader will 
	 * (stupidly) return a different instance of the case object, which fails the equality
	 * check because the default AnyRef implementation of equals is just this eq that.
	 * 
	 * The hashCode is computed at compile time, so this is safe.
	 * 
	 * DISREGARD THAT hashCodes collide because they are generated only based on the name
	 * of the current case object, so Module.Create.hashCode() == PersonalTutor.Create.hashCode()
	 */
	override def equals(other: Any) = other match {
		case that: BuiltInRoleDefinition => getName == that.getName
		case _ => false
	}
	override def hashCode() = getName.hashCode()
	override def toString() = getName
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

abstract class Role(@BeanProperty val scope: Option[PermissionsTarget]) {

	private var permissions: Map[Permission, Option[PermissionsTarget]] = ListMap()
	private var roles: Set[Role] = Set()
	
	def getName = getClass.getSimpleName
	def isScoped = scope.isDefined
	
	lazy val explicitPermissions = permissions
	lazy val explicitPermissionsAsList = explicitPermissions.toList
	lazy val subRoles = roles
			
	private def grant(scope: => Option[PermissionsTarget], perms: Iterable[Permission]): Unit =
		permissions ++= (perms map { _ -> scope })
		
	final def applyRoleDefinition(definition: RoleDefinition): Role = {
		permissions ++= definition.permissions(scope)
		roles ++= definition.subRoles(scope) 
			
		this
	}
}

abstract class BuiltInRole(scope: Option[PermissionsTarget], definition: BuiltInRoleDefinition) extends Role(scope) {
	def this(scope: PermissionsTarget, definition: BuiltInRoleDefinition) {
		this(Option(scope), definition)
	}
	
	applyRoleDefinition(definition)
}