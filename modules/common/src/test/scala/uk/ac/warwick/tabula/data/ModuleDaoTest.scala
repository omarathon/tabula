package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before

class ModuleDaoTest extends PersistenceTestBase {

	val dao = new ModuleDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	trait Context {
		// Already inserted by data.sql
		val cs108 = dao.getByCode("cs108").get
		val cs240 = dao.getByCode("cs240").get
		val cs241 = dao.getByCode("cs241").get
	}
	
	@Test def crud { transactional { tx =>
		new Context {
			dao.allModules should be (Seq(cs108, cs240, cs241))

			val cs333 = Fixtures.module("cs333")
			dao.saveOrUpdate(cs333)

			dao.allModules should be (Seq(cs108, cs240, cs241, cs333))

			dao.getByCode("cs333") should be (Some(cs333))
			dao.getByCode("wibble") should be (None)

			dao.getById(cs108.id) should be (Some(cs108))
			dao.getById(cs333.id) should be (Some(cs333))
			dao.getById("wibble") should be (None)
		}
	}}
	
}