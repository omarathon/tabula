package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.PersistenceTestBase
import org.springframework.orm.hibernate3.HibernateTemplate
import org.hibernate.Session
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection

class FunctionConversionsTest extends PersistenceTestBase with FunctionConversions {
	
	@Test def connectionCallback {
		val jdbc = new JdbcTemplate(dataSource)
		val fetchedString = jdbc.execute { connection: Connection =>
			"yes"
		}
		
		fetchedString should be ("yes")
	}
	
	@Test def hibernateCallback {
		val hibernate = new HibernateTemplate(sessionFactory)
		val fetchedString = hibernate.execute { session: Session =>
			"yes"
		}
		
		fetchedString should be ("yes")
	}

}