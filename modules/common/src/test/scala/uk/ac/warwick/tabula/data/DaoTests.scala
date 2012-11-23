package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.junit.ShouldMatchersForJUnit
import org.springframework.transaction.annotation.Transactional
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional


class DaoTests extends AppContextTestBase with ShouldMatchersForJUnit {
	@Autowired var deptDao:DepartmentDao =_
  
	@Transactional(readOnly=true)
	@Test def findDeptOwners {
	  val jeffsDepts = deptDao.getByOwner("cusfal")
	  jeffsDepts.size should be (1)
	  jeffsDepts.head.name should be ("Computer Science")
	  
	  val ronsDepts = deptDao.getByOwner("cuswizard")
	  ronsDepts should be ('empty)
	}
}