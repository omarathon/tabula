package uk.ac.warwick.tabula.data.model

import org.junit.Test
import uk.ac.warwick.tabula._
import collection.JavaConversions._
import uk.ac.warwick.tabula.helpers.ArrayList
import collection.JavaConverters._
import uk.ac.warwick.userlookup.Group
import uk.ac.warwick.userlookup.GroupImpl

class UserGroupTest extends PersistenceTestBase {

	@Test def membership {
		transactional { t =>
			var group = new UserGroup
			
			// users that can't be changed (i.e. as imported from upstream)
			group.staticIncludeUsers.addAll(Seq( "exoman", "eggdog" ))
			// users added manually
			group.includeUsers.addAll(Seq( "superhat", "menace" ))
			
			session.saveOrUpdate(group)
			session.flush
			session.clear
			
			group = session.get(classOf[UserGroup], group.id).asInstanceOf[UserGroup]
			
			group.staticIncludeUsers.size should be (2)
			group.staticIncludeUsers should (contain ("exoman") and contain ("eggdog"))
			
			group.includeUsers.size should be (2)
			group.includeUsers should (contain ("superhat") and contain ("menace"))
			
			group.excludeUser("eggdog") // poor eggdog.
			group.includes("whoareyou") should be (false)
			group.includes("exoman") should be (true)
			group.includes("eggdog") should be (false)
			group.includes("superhat") should be (true)
			group.includes("menace") should be (true)
			
			/* check that members works and is consistent.
			 * At time of writing, staticIncludeUsers would get
			 * added to includeUsers each time :|
			 */
			group.members.size should be (3)
			group.members.size should be (3)
			group.members.size should be (3)
		}
	} 
	
	@Test def withWebgroup {
		val userLookup = new MockUserLookup
		
		val group = new UserGroup
		group.userLookup = userLookup
		
		group.addUser("cuscav")
		group.addUser("curef")
		group.excludeUser("cusmab") // we don't like Steve
		group.staticIncludeUsers.add("sb_systemtest")
		group.baseWebgroup = "in-elab"
			
		val webgroup = new GroupImpl
		webgroup.setUserCodes(List("cuscav", "cusmab", "cusebr").asJava)
		
		userLookup.groupService.groupMap += ("in-elab" -> webgroup)
			
		group.members should be (Seq("cuscav", "curef", "sb_systemtest", "cuscav", "cusebr"))
	}
	
	@Test def copy {
		val group = new UserGroup
		group.addUser("cuscav")
		group.addUser("curef")
		group.excludeUser("cusmab") // we don't like Steve
		group.staticIncludeUsers.add("sb_systemtest")
		group.baseWebgroup = "in-elab"
		group.universityIds = true
			
		val group2 = new UserGroup
		group2.copyFrom(group)
		
		group.eq(group2) should be (false)
		
		group2.includeUsers.asScala.toSeq should be (group.includeUsers.asScala.toSeq)
		group2.excludeUsers.asScala.toSeq should be (group.excludeUsers.asScala.toSeq)
		group2.staticIncludeUsers.asScala.toSeq should be (group.staticIncludeUsers.asScala.toSeq)
		group2.baseWebgroup should be (group.baseWebgroup)
		group2.universityIds should be (group.universityIds)
	}
	
}