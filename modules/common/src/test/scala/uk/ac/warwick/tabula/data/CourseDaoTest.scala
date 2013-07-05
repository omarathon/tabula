package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.Fixtures

class CourseDaoTest extends AppContextTestBase {

	@Autowired var dao:CourseDao =_

	@Test def crud = transactional { tx =>
		val course = Fixtures.course("TPOS-M9P0")
		dao.saveOrUpdate(course)

		dao.getByCode("TPOS-M9P0") should be (Some(course))
		dao.getByCode("wibble") should be (None)
	}

}