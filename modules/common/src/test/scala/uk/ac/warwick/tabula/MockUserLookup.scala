package uk.ac.warwick.tabula
import scala.collection.JavaConversions._
import org.apache.commons.lang3.StringUtils._
import uk.ac.warwick.tabula.services.{UserByWarwickIdCache, UserLookupService}
import uk.ac.warwick.userlookup._
import uk.ac.warwick.userlookup.webgroups.GroupInfo
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException
import uk.ac.warwick.tabula.JavaImports._
import scala.Some
import scala.util.Random
import java.security.MessageDigest
import sun.misc.BASE64Encoder
import uk.ac.warwick.tabula.data.model.Gender.{Female, Male}
import uk.ac.warwick.tabula.UserFlavour.{Applicant, Anonymous, Unverified, Vanilla}

class MockUserLookup(var defaultFoundUser: Boolean)
	extends UserLookupAdapter(null)
	with UserLookupService
	with JavaImports
	with MockUser {

	def this() = {
		this(false)
	}

	var users: Map[String, User] = Map()
	var filterUserResult: JList[User] = JArrayList()

	var findUsersEnabled: Boolean = false

	val groupService: MockGroupService = new MockGroupService

	override def getGroupService() = groupService

    def addFindUsersWithFilterResult(user: User) {
        filterUserResult += user
    }

    override def getUserByUserId(theUserId: String) = {
    	users.get(theUserId).getOrElse {
    		if (defaultFoundUser) {
    			val user = new User()
    			user.setUserId(theUserId)
    			user.setFoundUser(true)
    			user
    		} else {
    			new AnonymousUser
    		}
    	}
    }

    override def getUserByWarwickUniId(warwickId: String) =
    	users.values.find(_.getWarwickId() == warwickId).getOrElse {
    		if (defaultFoundUser) {
    			val user = new User()
    			user.setWarwickId(warwickId)
    			user.setFoundUser(true)
    			user
    		} else {
    			new AnonymousUser
    		}
		}

    override def getUserByWarwickUniId(warwickId: String, includeDisabledLogins: Boolean) = getUserByWarwickUniId(warwickId)

    override def findUsersWithFilter(filterValues: JMap[String, String]) = {
        if (findUsersEnabled) {
            val list: JList[User] = JArrayList()
            list.addAll(filterUserResult)
            list
        } else {
            throw new UnsupportedOperationException()
        }
    }

    override def getUsersByUserIds(userIds: JList[String]): JMap[String, User] = {
    	val map: Map[String, User] = (userIds.toList map { id => (id -> getUserByUserId(id) )}).toMap

    	map
    }

    /**
     * Method to quickly add some mock users who exist and definitely
     * have genuine, real names.
     */
    def registerUsers(userIds: String*) {
        for (id <- userIds) {
            val user = mockUser(id);
            users += (id -> user)
        }
    }

    def registerUserObjects(u: User*) {
    	for (user <- u) {
    		users += (user.getUserId -> user)
    	}
    }

}


class MockCachingLookupService(var flavour: UserFlavour = Vanilla)
	extends UserLookupAdapter(null)
	with UserLookupService
	with UserByWarwickIdCache
	with MockUser {

	private val sha = MessageDigest.getInstance("SHA-1")
	private val b64 = new BASE64Encoder()

	override def getUserByWarwickUniIdUncached(warwickId: String): User = {
		flavour match {
			case Unverified => new UnverifiedUser(new UserLookupException)
			case Anonymous => new AnonymousUser()
			case Applicant => {
				val user = new AnonymousUser()
				user.setWarwickId(warwickId)
				user
			}
			case Vanilla => {
				// hash WarwickId to a consistent 6 char 'usercode'. Tiny, but finite risk of collision/short usercodes.
				val userId = b64.encode(sha.digest(warwickId.getBytes)).filter(_.isLower).take(6)
				val user = mockUser(userId)
				user.setWarwickId(warwickId)
				user
			}
		}
	}

	override def getUserByWarwickUniId(id: String) = getUserByWarwickUniId(id, true)

	override def getUserByWarwickUniId(id: String, ignored: Boolean) = UserByWarwickIdCache.get(id)
}


trait MockUser {
	def mockUser(id: String): User = {
		val user = new User(id)
		user.setFoundUser(true)
		user.setVerified(true)
		user.setFirstName(capitalize(id)+"y");
		user.setLastName("Mc"+capitalize(id)+"erson");
		user.setDepartment("Department of " + capitalize(id));
		user.setShortDepartment("Dept of " + capitalize(id));
		user.setDepartmentCode("D" + capitalize(id.substring(0, 1)));
		user.setEmail(id + "@example.com");
		user
	}
}

/* Vanilla is a verified user, whose session was completely retrieved from SSO.

 * Anonymous means user is *not* logged into SSO, but *is*
 * implicitly verified - as anonymous - with default properties set,
 * including warwickId of empty string.
 *
 * Unverified means a UserLookupException has been raised, which should
 * return an UnverifiedUser, a subclass of AnonymousUser;
 * most properties should have default properties set too.
 *
 * Applicant is another special case of AnonymousUser, this time with
 * a real warwickID set.
 */
sealed abstract class UserFlavour
object UserFlavour {
	case object Anonymous extends UserFlavour
	case object Unverified extends UserFlavour
	case object Applicant extends UserFlavour
	case object Vanilla extends UserFlavour
}


class MockGroupService extends GroupService {
	var groupMap: Map[String, Group] = Map()
	var usersInGroup: Map[(String, String), Boolean] = Map() withDefaultValue(false)
	var groupNamesForUserMap: Map[String, Seq[String]] = Map()

	override def getGroupByName(groupName: String) = groupMap.get(groupName).getOrElse {
		throw new GroupNotFoundException(groupName)
	}

    override def getGroupsForDeptCode(deptCode: String) = ???

    override def getGroupsForQuery(query: String) = ???

    override def getGroupsForUser(user: String) = ???

    override def getGroupsNamesForUser(user: String) =
    	groupNamesForUserMap.get(user) match {
    		case Some(groups) => groups
    		case _ => JArrayList[String]()
    	}

    override def getRelatedGroups(groupName: String) = ???

    override def getUserCodesInGroup(groupName: String) = {
        try {
            getGroupByName(groupName).getUserCodes()
        } catch {
          	case e: GroupNotFoundException => List()
        }
    }

    override def isUserInGroup(user: String, group: String) = usersInGroup((user, group))

    override def setTimeoutConfig(config: WebServiceTimeoutConfig) {}

    override def getGroupInfo(name: String) = new GroupInfo(getGroupByName(name).getUserCodes().size())

    override def clearCaches() = ???
    override def getCaches() = ???

}