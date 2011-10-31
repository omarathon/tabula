package uk.ac.warwick.courses.data
import org.junit.Test
import uk.ac.warwick.courses.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.junit.ShouldMatchersForJUnit
import org.springframework.transaction.annotation.Transactional


class DaoTests extends AppContextTestBase with ShouldMatchersForJUnit {
	@Autowired var deptDao:DepartmentDao =_
  
	@Transactional(readOnly=true)
	@Test def findDeptOwners {
	  val jeffsDepts = deptDao.getByOwner("cusfal")
	  jeffsDepts.size should be (1)
	  jeffsDepts.first.name should be ("Computer Science")
	  
	  val ronsDepts = deptDao.getByOwner("cuswizard")
	  ronsDepts should be ('empty)
	}
}