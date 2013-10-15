package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.{AutowiringPermissionsDaoComponent, PermissionsDaoComponent, PermissionsDao}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect._
import uk.ac.warwick.userlookup.GroupService
import scala.collection.JavaConverters._
import uk.ac.warwick.util.cache.{SingularCacheEntryFactory, CacheEntryFactory, Caches}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.queue.conversion.ItemType
import org.codehaus.jackson.annotate.JsonAutoDetect
import uk.ac.warwick.util.queue.QueueListener
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.helpers.RequestLevelCaching

trait PermissionsService {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])

	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]]
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]]
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]]
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]]
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]]
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Stream[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Stream[GrantedPermission[A]]
	
	def getAllPermissionDefinitionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser, targetPermission: Permission): Set[A]
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup

	def getCustomRoleDefinitionsBasedOn(roleDefinition:BuiltInRoleDefinition):Seq[CustomRoleDefinition]
}

@Service(value = "permissionsService")
class AutowiringPermissionsServiceImpl extends PermissionsServiceImpl with AutowiringPermissionsDaoComponent with PermissionsServiceCachesImpl


class PermissionsServiceImpl extends PermissionsService with Logging
	with QueueListener with InitializingBean
	with GrantedRolesForUserCache
	with GrantedRolesForGroupCache
	with GrantedPermissionsForUserCache
	with GrantedPermissionsForGroupCache {
	this:PermissionsDaoComponent with PermissionsServiceCaches=>


	var groupService = Wire[GroupService]
	var queue = Wire.named[Queue]("settingsSyncTopic")
	

	override def isListeningToQueue = true
	override def onReceive(item: Any) {	
		item match {
				case copy: PermissionsCacheBusterMessage => clearCaches()
				case _ =>
		}
	}
		
	override def afterPropertiesSet() {
		queue.addListener(classOf[PermissionsCacheBusterMessage].getAnnotation(classOf[ItemType]).value, this)
	}
	
	private def clearCaches() {
		// This is monumentally dumb. There's a more efficient way than this!
		GrantedRolesForUserCache.clear()
		GrantedRolesForGroupCache.clear()
		GrantedPermissionsForUserCache.clear()
		GrantedPermissionsForGroupCache.clear()
	}
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = permissionsDao.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = {
		permissionsDao.saveOrUpdate(permission)
		clearCaches()
		queue.send(new PermissionsCacheBusterMessage)
	}
	def saveOrUpdate(role: GrantedRole[_]) = {
		permissionsDao.saveOrUpdate(role)
		clearCaches()
		queue.send(new PermissionsCacheBusterMessage)
	}
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]] = 
		transactional(readOnly = true) {
			roleDefinition match {
				case builtIn: BuiltInRoleDefinition => permissionsDao.getGrantedRole(scope, builtIn)
				case custom: CustomRoleDefinition => permissionsDao.getGrantedRole(scope, custom)
				case _ => None
			}
		}
	
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] =
		transactional(readOnly = true) {
			permissionsDao.getGrantedPermission(scope, permission, overrideType)
		}
	
	private def ensureFoundUserSeq[A](user: CurrentUser)(fn: => Seq[A]): Seq[A] =
		if (user.exists) fn
		else Seq.empty
	
	private def ensureFoundUserSet[A](user: CurrentUser)(fn: => Set[A]): Set[A] =
		if (user.exists) fn
		else Set.empty
	
	private def ensureFoundUserStream[A](user: CurrentUser)(fn: => Stream[A]): Stream[A] =
		if (user.exists) fn
		else Stream.empty
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]] = ensureFoundUserSeq(user)(transactional(readOnly = true) {
		permissionsDao.getGrantedRolesFor(scope) filter { _.users.includes(user.apparentId) }
	})
	
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]] =
		ensureFoundUserSeq(user)(transactional(readOnly = true) {
			permissionsDao.getGrantedPermissionsFor(scope).toStream filter { _.users.includes(user.apparentId) }
		}
	)
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]] = ensureFoundUserSeq(user)(getGrantedRolesFor[PermissionsTarget](user))
	
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]] = ensureFoundUserSeq(user)(getGrantedPermissionsFor[PermissionsTarget](user))
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Stream[GrantedRole[A]] =
		ensureFoundUserStream(user)(transactional(readOnly = true) {
			val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala

			rolesByIdCache.getGrantedRolesByIds[A](
				// Get all roles where usercode is included,
				GrantedRolesForUserCache.get((user.apparentUser, classTag[A])).asScala

				// Get all roles backed by one of the webgroups,
				++ (GrantedRolesForGroupCache.get((groupNames, classTag[A])).asScala)
			).toStream
				// For sanity's sake, filter by the users including the user
				.filter { _.users.includes(user.apparentId) }
		}
	)
	
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Stream[GrantedPermission[A]] =
		ensureFoundUserStream(user)(transactional(readOnly = true) {
			val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala

			permissionsByIdCache.getGrantedPermissionsByIds[A](
				// Get all permissions where usercode is included,
				GrantedPermissionsForUserCache.get((user.apparentUser, classTag[A])).asScala

				// Get all permissions backed by one of the webgroups,
				++ (GrantedPermissionsForGroupCache.get((groupNames, classTag[A])).asScala )
			).toStream
				// For sanity's sake, filter by the users including the user
				.filter { _.users.includes(user.apparentId) }
		}
	)
	
	def getAllPermissionDefinitionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser, targetPermission: Permission): Set[A] = ensureFoundUserSet(user) {
		val scopesWithGrantedRole = 
			getGrantedRolesFor[A](user)
			.filter { _.mayGrant(targetPermission) }
			.map { _.scope }
			
		val scopesWithGrantedPermission =
			getGrantedPermissionsFor[A](user)
			.filter { perm => perm.overrideType == GrantedPermission.Allow && perm.permission == targetPermission }
			.map { _.scope }
			
		Set() ++ scopesWithGrantedRole ++ scopesWithGrantedPermission
	}
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup = transactional() {
		getGrantedRole(scope, roleDefinition) match {
			case Some(role) => role.users
			case _ => {
				val role = GrantedRole(scope, roleDefinition)

				permissionsDao.saveOrUpdate(role)
				role.users
			}
		}
	}

	def getCustomRoleDefinitionsBasedOn(roleDefinition: BuiltInRoleDefinition): Seq[CustomRoleDefinition] = {
		permissionsDao.getCustomRoleDefinitionsBasedOn(roleDefinition)
	}
}

class GrantedPermissionsByIdCache(dao: PermissionsDao) extends RequestLevelCaching[String, Option[GrantedPermission[_]]] {
	
	def getGrantedPermissionsByIds[A <: PermissionsTarget : ClassTag](ids: Seq[String]) =
		ids flatMap { id => getGrantedPermissionsById[A](id) }
	
	def getGrantedPermissionsById[A <: PermissionsTarget : ClassTag](id: String) = 
		cachedBy(id) { dao.getGrantedPermission[A](id) }.asInstanceOf[Option[GrantedPermission[A]]]
	
}

class GrantedRoleByIdCache(dao: PermissionsDao) extends RequestLevelCaching[String, Option[GrantedRole[_]]] {
	
	def getGrantedRolesByIds[A <: PermissionsTarget : ClassTag](ids: Seq[String]) =
		ids flatMap { id => getGrantedRoleById[A](id) }
	
	def getGrantedRoleById[A <: PermissionsTarget : ClassTag](id: String) =
		cachedBy(id) { dao.getGrantedRole[A](id) }.asInstanceOf[Option[GrantedRole[A]]]
	
}

/*
 * All caches map from a combination of the class tag for the scope, and the user (or webgroup name) 
 * and map to a list of IDs of the granted roles / permissions.
 */ 

trait GrantedRolesForUserCache { self: PermissionsDaoComponent =>
	final val GrantedRolesForUserCacheName = "GrantedRolesForUser"
	final val GrantedRolesForUserCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedRolesForUserCacheMaxSize = 1000
	
	final val GrantedRolesForUserCache = 
		Caches.newCache(GrantedRolesForUserCacheName, new GrantedRolesForUserCacheFactory, GrantedRolesForUserCacheMaxAgeSecs)
	GrantedRolesForUserCache.setMaxSize(GrantedRolesForUserCacheMaxSize)
	
	class GrantedRolesForUserCacheFactory extends CacheEntryFactory[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (User, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (user, tag) => JArrayList(permissionsDao.getGrantedRolesForUser(user)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
		
		override def isSupportsMultiLookups() = false
		def create(cacheKeys: JList[(User, ClassTag[_ <: PermissionsTarget])]): JMap[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] = {
			throw new UnsupportedOperationException("Multi lookups not supported")
		}
	}
}

trait GrantedRolesForGroupCache { self: PermissionsDaoComponent =>
	final val GrantedRolesForGroupCacheName = "GrantedRolesForGroup"
	final val GrantedRolesForGroupCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedRolesForGroupCacheMaxSize = 1000
	
	final val GrantedRolesForGroupCache = 
		Caches.newCache(GrantedRolesForGroupCacheName, new GrantedRolesForGroupCacheFactory, GrantedRolesForGroupCacheMaxAgeSecs)
	GrantedRolesForGroupCache.setMaxSize(GrantedRolesForGroupCacheMaxSize)
	
	class GrantedRolesForGroupCacheFactory extends SingularCacheEntryFactory[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (groupNames, tag) => JArrayList(permissionsDao.getGrantedRolesForWebgroups(groupNames)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
	}
}

trait GrantedPermissionsForUserCache { self: PermissionsDaoComponent =>
	final val GrantedPermissionsForUserCacheName = "GrantedPermissionsForUser"
	final val GrantedPermissionsForUserCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedPermissionsForUserCacheMaxSize = 1000
	
	final val GrantedPermissionsForUserCache = 
		Caches.newCache(GrantedPermissionsForUserCacheName, new GrantedPermissionsForUserCacheFactory, GrantedPermissionsForUserCacheMaxAgeSecs)
	GrantedPermissionsForUserCache.setMaxSize(GrantedPermissionsForUserCacheMaxSize)
	
	class GrantedPermissionsForUserCacheFactory extends SingularCacheEntryFactory[(User, ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (User, ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (user, tag) => JArrayList(permissionsDao.getGrantedPermissionsForUser(user)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
	}
}

trait GrantedPermissionsForGroupCache { self: PermissionsDaoComponent =>
	final val GrantedPermissionsForGroupCacheName = "GrantedPermissionsForGroup"
	final val GrantedPermissionsForGroupCacheMaxAgeSecs = 60 * 60 // 1 hour
	final val GrantedPermissionsForGroupCacheMaxSize = 1000
	
	final val GrantedPermissionsForGroupCache = 
		Caches.newCache(GrantedPermissionsForGroupCacheName, new GrantedPermissionsForGroupCacheFactory, GrantedPermissionsForGroupCacheMaxAgeSecs)
	GrantedPermissionsForGroupCache.setMaxSize(GrantedPermissionsForGroupCacheMaxSize)
	
	class GrantedPermissionsForGroupCacheFactory extends SingularCacheEntryFactory[(Seq[String], ClassTag[_ <: PermissionsTarget]), JArrayList[String]] {
		def create(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget])) = cacheKey match {
			case (groupNames, tag) => JArrayList(permissionsDao.getGrantedPermissionsForWebgroups(groupNames)(tag).map { role => role.id }.asJava)
		}
		def shouldBeCached(ids: JArrayList[String]) = true
	}
}
trait PermissionsServiceCaches {
	val rolesByIdCache:GrantedRoleByIdCache
	val permissionsByIdCache:GrantedPermissionsByIdCache
}
trait PermissionsServiceCachesImpl extends PermissionsServiceCaches {
	this:PermissionsDaoComponent=>
	val rolesByIdCache:GrantedRoleByIdCache = new GrantedRoleByIdCache(permissionsDao)
	val permissionsByIdCache = new GrantedPermissionsByIdCache(permissionsDao)
}


@ItemType("PermissionsCacheBuster")
@JsonAutoDetect
class PermissionsCacheBusterMessage