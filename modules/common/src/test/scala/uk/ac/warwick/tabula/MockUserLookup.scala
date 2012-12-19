package uk.ac.warwick.tabula

import org.apache.commons.lang3.StringUtils._
import uk.ac.warwick.userlookup.UserLookupAdapter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.GroupService
import uk.ac.warwick.userlookup.Group
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig
import uk.ac.warwick.userlookup.webgroups.GroupInfo
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException
import scala.collection.JavaConversions._
import uk.ac.warwick.userlookup.AnonymousUser

class MockUserLookup(var defaultFoundUser: Boolean) extends UserLookupAdapter(null) {
	def this() {
		this(false)
	}
  
	val users: Map[String, User] = Map()
	val filterUserResult: List[User] = List()
	
	var findUsersEnabled: Boolean = false
	
	val groupService: MockGroupService = new MockGroupService
	
	override def getGroupService() = groupService

    def addFindUsersWithFilterResult(userId: User) {
        filterUserResult.add(userId);
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

    def findUsersWithFilter(filterValues: Map[String, String]) = {
        if (findUsersEnabled) {
            filterUserResult
        } else {
            throw new UnsupportedOperationException()
        }
    }

    def getUsersByUserIds(userIds: List[String]) = {
    	val users: Map[String, User] = Map()
    	for (id <- userIds) {
    		users.put(id, getUserByUserId(id))
    	}
    	users
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
            users.put(id, user);
        }
    }

}

class MockGroupService extends GroupService {
	val DoItYourselfKevin = "Not implemented - add to mock object if you need it"
	var groupMap: Map[String, Group] = Map()
	
	override def getGroupByName(groupName: String) = groupMap.get(groupName).getOrElse {
		throw new GroupNotFoundException(groupName)
	}

    override def getGroupsForDeptCode(deptCode: String) = {
        throw new UnsupportedOperationException(DoItYourselfKevin)
    }

    override def getGroupsForQuery(query: String) = {
        throw new UnsupportedOperationException(DoItYourselfKevin)
    }

    override def getGroupsForUser(user: String) = {
        throw new UnsupportedOperationException(DoItYourselfKevin)
    }

    override def getGroupsNamesForUser(user: String) = {
        throw new UnsupportedOperationException(DoItYourselfKevin)
    }

    override def getRelatedGroups(groupName: String) = {
        throw new UnsupportedOperationException(DoItYourselfKevin)
    }
    
    override def getUserCodesInGroup(groupName: String) = {
        try {
            getGroupByName(groupName).getUserCodes()
        } catch {
          	case e: GroupNotFoundException => List()
        }
    }

    override def isUserInGroup(user: String, group: String) = false

    override def setTimeoutConfig(config: WebServiceTimeoutConfig) {}

    override def getGroupInfo(name: String) = new GroupInfo(getGroupByName(name).getUserCodes().size())
	
}