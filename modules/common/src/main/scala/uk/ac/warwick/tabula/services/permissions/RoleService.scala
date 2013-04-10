package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.immutable.ListMap
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission

trait RoleProvider {
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[Role]
	
	def rolesProvided: Set[Class[_ <: Role]]
	
	/**
	 * Override and return true if this service is exhaustive - i.e. you should continue to interrogate it even after it has returned results
	 */
	def isExhaustive = false
}

trait ScopelessRoleProvider extends RoleProvider {
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = getRolesFor(user)
	
	def getRolesFor(user: CurrentUser): Seq[Role]
}

case class PermissionDefinition(permission: Permission, scope: Option[PermissionsTarget], permissionType: GrantedPermission.OverrideType)

trait PermissionsProvider {
	def getPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Stream[PermissionDefinition]
	
	/**
	 * Override and return true if this service is exhaustive - i.e. you should continue to interrogate it even after it has returned results
	 */
	def isExhaustive = false
}

trait RoleService {
	def getExplicitPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Stream[PermissionDefinition]
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role]
	def hasRole(user: CurrentUser, role: Role): Boolean
}

@Service
class RoleServiceImpl extends RoleService with Logging {
	
	/** Spring should wire in all beans that extend RoleProvider */
	@Autowired var roleProviders: Array[RoleProvider] = Array()
	
	/** Spring should wire in all beans that extend PermissionsProvider */
	@Autowired var permissionsProviders: Array[PermissionsProvider] = Array()
	
	def getExplicitPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Stream[PermissionDefinition] = {
		def streamScoped(providers: Stream[PermissionsProvider], scope: PermissionsTarget): Stream[PermissionDefinition] = {
			if (scope == null) Stream.empty
			else {
				val results = providers.toStream map { provider => (provider, provider.getPermissionsFor(user, scope)) }
				val (hasResults, noResults) = results.partition { !_._2.isEmpty }

				val stream = hasResults flatMap { _._2 }
				val next = scope.permissionsParents.toStream flatMap { streamScoped((noResults #::: (hasResults filter { _._1.isExhaustive })) map {_._1}, _) }

				stream #::: next
			}
		}
		
		streamScoped(permissionsProviders.toStream, scope)
	}
		
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = {	
		// Split providers into Scopeless and scoped
		val (scopeless, scoped) = roleProviders.partition(_.isInstanceOf[ScopelessRoleProvider])
		
		// We only need to do scopeless once
		val scopelessStream = scopeless.toStream flatMap { _.asInstanceOf[ScopelessRoleProvider].getRolesFor(user) }
		
		/* We don't want to needlessly continue to interrogate scoped providers even after they 
		 * have returned something that isn't an empty Seq. Anything that isn't an empty Seq 
		 * can be treated as the final action of this provider EXCEPT in the case of the custom
		 * role provider, so we special-case that */  
		def streamScoped(providers: Stream[RoleProvider], scope: PermissionsTarget): Stream[Role] = {
			if (scope == null) Stream.empty
			else {
				val results = providers.toStream map { provider => (provider, provider.getRolesFor(user, scope)) }
				val (hasResults, noResults) = results.partition { !_._2.isEmpty }

				val stream = hasResults flatMap { _._2 }
				val next = scope.permissionsParents.toStream flatMap { streamScoped((noResults #::: (hasResults filter { _._1.isExhaustive })) map {_._1}, _) }

				stream #::: next
			}
		}
				
		scopelessStream #::: streamScoped(scoped.toStream, scope)
	}
	
	def hasRole(user: CurrentUser, role: Role) = {
		val targetClass = role.getClass
		
		// Go through the list of RoleProviders and get any that provide this role
		val allRoles = roleProviders.filter(_.rolesProvided contains targetClass) flatMap { 
			case scopeless: ScopelessRoleProvider => scopeless.getRolesFor(user)
			case provider if role.scope.isDefined => provider.getRolesFor(user, role.scope.get)
			case _ => Seq()
		}
		
		allRoles contains(role)
	}

}
