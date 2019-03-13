package uk.ac.warwick.tabula.data.model.permissions

import javax.persistence._
import org.hibernate.annotations.{Any, AnyMetaDef, MetaValue, Type}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}

import scala.reflect._

@Entity
@Access(AccessType.FIELD)
class GrantedPermission[A <: PermissionsTarget] extends GeneratedId with HibernateVersioned with PostLoadBehaviour {
	import GrantedPermission.OverrideType

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.EAGER)
	@JoinColumn(name="usergroup_id")
	private var _users: UserGroup = UserGroup.ofUsercodes
	def users: UnspecifiedTypeUserGroup = _users

	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	var permission: Permission = _

	/*
	 * TODO Deny not currently supported by SecurityService because the value isn't passed through!
	 */
	var overrideType: OverrideType = _

	@Column(name = "scope_type", insertable = false, updatable = false)
	var scopeType: String = _

	@Any(metaColumn = new Column(name = "scope_type"), fetch = FetchType.LAZY)
	@AnyMetaDef(idType = "string", metaType = "string",
		metaValues = Array(
			new MetaValue(targetEntity = classOf[Department], value = "Department"),
			new MetaValue(targetEntity = classOf[Module], value = "Module"),
			new MetaValue(targetEntity = classOf[Route], value = "Route"),
			new MetaValue(targetEntity = classOf[Member], value = "Member"),
			new MetaValue(targetEntity = classOf[Assignment], value = "Assignment"),
			new MetaValue(targetEntity = classOf[SmallGroup], value = "SmallGroup"),
			new MetaValue(targetEntity = classOf[SmallGroupSet], value = "SmallGroupSet"),
			new MetaValue(targetEntity = classOf[SmallGroupEvent], value = "SmallGroupEvent"),
		)
	)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: A = _

	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad() {
		ensureUsers
	}

	def ensureUsers: UnspecifiedTypeUserGroup = {
		if (users == null) _users = UserGroup.ofUsercodes
		users
	}

}

object GrantedPermission {
	type OverrideType = Boolean
	val Allow: OverrideType = true
	val Deny: OverrideType = false

	def apply[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: OverrideType): GrantedPermission[A] =
		if (canDefineFor[A]) {
			val p = new GrantedPermission[A]
			p.scope = scope
			p.permission = permission
			p.overrideType = overrideType
			p
		} else {
			throw new IllegalArgumentException(s"Cannot define new permissions for $scope")
		}

	def canDefineFor[A <: PermissionsTarget : ClassTag]: Boolean = classTag[A] == classTag[PermissionsTarget] || scopeType[A].nonEmpty
	def scopeType[A <: PermissionsTarget : ClassTag]: Option[String] = classTag[A] match {
		case t if isSubtype(t, classTag[Department]) => Some("Department")
		case t if isSubtype(t, classTag[Module]) => Some("Module")
		case t if isSubtype(t, classTag[Route]) => Some("Route")
		case t if isSubtype(t, classTag[Member]) => Some("Member")
		case t if isSubtype(t, classTag[Assignment]) => Some("Assignment")
		case t if isSubtype(t, classTag[SmallGroup]) => Some("SmallGroup")
		case t if isSubtype(t, classTag[SmallGroupSet]) => Some("SmallGroupSet")
		case t if isSubtype(t, classTag[SmallGroupEvent]) => Some("SmallGroupEvent")
		case _ => None
	}

	private def isSubtype[A,B](self: ClassTag[A], other: ClassTag[B]) = other.runtimeClass.isAssignableFrom(self.runtimeClass)
}
