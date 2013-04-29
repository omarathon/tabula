package uk.ac.warwick.tabula
import scala.collection.JavaConversions._
import org.apache.commons.lang3.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.Group
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.UserLookupAdapter
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig
import uk.ac.warwick.userlookup.webgroups.GroupInfo
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException
import uk.ac.warwick.tabula.JavaImports._

class MockUserLookup(var defaultFoundUser: Boolean) extends UserLookupAdapter(null) with UserLookupService with JavaImports {
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
            val user = new User(id);
            user.setFoundUser(true);
            user.setFirstName(capitalize(id)+"y");
            user.setLastName("Mc"+capitalize(id)+"erson");
            user.setDepartment("Department of " + capitalize(id));
            user.setShortDepartment("Dept of " + capitalize(id));
            user.setDepartmentCode("D" + capitalize(id.substring(0, 1)));
            user.setEmail(id + "@example.com");
            users += (id -> user)
        }
    }

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
	
}