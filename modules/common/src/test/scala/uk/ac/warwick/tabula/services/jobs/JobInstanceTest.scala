package uk.ac.warwick.tabula.services.jobs
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.system.CurrentUserInterceptor
import uk.ac.warwick.tabula.services.permissions.RoleService

class JobInstanceTest extends TestBase with Mockito {
	
	val jsonMapper = new JsonObjectMapperFactory().createInstance
	val userLookup = new MockUserLookup
	
	val currentUserFinder = new CurrentUserInterceptor
	val roleService = mock[RoleService]
	currentUserFinder.userLookup = userLookup
	currentUserFinder.roleService = roleService

	@Test def onLoad {
		val instance = new JobInstanceImpl
		instance.jsonMapper = jsonMapper
		instance.userLookup = userLookup
		instance.currentUserFinder = currentUserFinder
		
		userLookup.registerUsers("cuscav", "cusebr")
		
		instance.realUser = "cuscav"
		instance.apparentUser = "cusebr"
			
		instance.data = """{"steve":"yes"}"""
			
		instance.user should be (null)
		instance.json should be ('empty)
		instance.updatedDate should not be (null)
		
		val oldUpdatedDate = instance.updatedDate
		instance.postLoad
		
		instance.updatedDate.isAfter(oldUpdatedDate) should be (true)
		instance.user.apparentId should be ("cusebr")
		instance.user.realId should be ("cuscav")
		instance.json("steve") should be ("yes")
	}

}