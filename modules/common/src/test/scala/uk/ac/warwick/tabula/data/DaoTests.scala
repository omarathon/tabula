package uk.ac.warwick.tabula.data

import org.junit.Test
import org.scalatest.junit.ShouldMatchersForJUnit

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.helpers.Logging

class DaoTests extends AppContextTestBase with ShouldMatchersForJUnit with Logging {
	lazy val deptDao = Wire[DepartmentDao]
	lazy val memberDao = Wire[MemberDao]
	
	@Test def findDeptOwners = transactional { tx =>
	  val jeffsDepts = deptDao.getByOwner("cusfal")
	  jeffsDepts.size should be (1)
	  jeffsDepts.head.name should be ("Computer Science")
	  
	  val ronsDepts = deptDao.getByOwner("cuswizard")
	  ronsDepts should be ('empty)
	}
	
/*	
	@Test def testSomething = transactional { tx =>
		val group = new UserGroup()
		group.addUser("1112939")
		group.universityIds = true
		
		val module = new Module()
		module.code="en107"
		
		val upAssessGroup = new UpstreamAssessmentGroup()
		upAssessGroup.moduleCode = "en107"
		upAssessGroup.members = group
			
		session.saveOrUpdate(group)
		session.saveOrUpdate(module)
		session.saveOrUpdate(upAssessGroup)
		
		session.flush
		session.clear
		
		val modules = memberDao.getSomethingForTesting
		logger.debug("found modules: " + modules)
	  modules.size should be (1)
	}	
	*/
	
	@Test def findRegisteredModules = transactional { tx =>
		val group = new UserGroup()

		group.staticIncludeUsers.add("1112939")
		group.universityIds = true
		val module = new Module()
		module.code="en107"
		
		val upAssessGroup = new UpstreamAssessmentGroup()
		upAssessGroup.moduleCode = "EN107-15"
		upAssessGroup.members = group
		
		session.saveOrUpdate(group)
		session.saveOrUpdate(module)
		session.saveOrUpdate(upAssessGroup)
		
	  val modules = memberDao.getRegisteredModules("1112939")
	  modules.size should be (1)
	  modules.head.code should be ("en107")
	}
}
