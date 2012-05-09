package uk.ac.warwick.courses.data.model

import org.junit.Test
import uk.ac.warwick.courses._
import collection.JavaConversions._
import uk.ac.warwick.courses.helpers.ArrayList

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
		}
	} 
	
}