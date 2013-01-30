package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.helpers.Logging

trait RoleProvider {
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role]
	
	def rolesProvided: Set[Class[R] forSome { type R <: Role }]
}

trait ScopelessRoleProvider extends RoleProvider {
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget) = getRolesFor(user)
	
	def getRolesFor(user: CurrentUser): Seq[Role]
}

@Service
class RoleService extends Logging {
	
	/** Spring should wire in all beans that extend RoleProvider */
	@Autowired var providers: Array[RoleProvider] = Array()
	
	// TAB-19 Not yet implemented
	def getExplicitPermissionsFor(user: CurrentUser): Map[Permission, Option[PermissionsTarget]] = Map()
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget) = {	
		// Split providers into Scopeless and scoped
		val (scopeless, scoped) = providers.partition(_.isInstanceOf[ScopelessRoleProvider])
		
		// We only need to do scopeless once
		val scopelessStream = scopeless.toStream flatMap { _.asInstanceOf[ScopelessRoleProvider].getRolesFor(user) }
		
		/* We don't want to needlessly continue to interrogate scoped providers even after they 
		 * have returned something that isn't an empty Seq. Anything that isn't an empty Seq 
		 * can be treated as the final action of this provider. */  
		def streamScoped(providers: Stream[RoleProvider], scope: => PermissionsTarget): Stream[Role] = {
			val results = providers.toStream map { provider => (provider, provider.getRolesFor(user, scope)) }
			val (hasResults, noResults) = results.partition { !_._2.isEmpty }
			
			val stream = hasResults flatMap { _._2 }
			val next = scope.permissionsParents.toStream flatMap { streamScoped(noResults map {_._1}, _) }
			
			stream #::: next
		}
				
		scopelessStream #::: streamScoped(scoped.toStream, scope)
	}
	
	def hasRole(user: CurrentUser, role: Role, scope: => Option[PermissionsTarget]) = {
		val targetClass = role.getClass
		
		// Go through the list of RoleProviders and get any that provide this role
		val allRoles = providers.filter(_.rolesProvided contains targetClass) flatMap { _ match {
			case scopeless: ScopelessRoleProvider => scopeless.getRolesFor(user)
			case provider => provider.getRolesFor(user, scope.get)
		}}
		
		allRoles contains(role)
	}

}