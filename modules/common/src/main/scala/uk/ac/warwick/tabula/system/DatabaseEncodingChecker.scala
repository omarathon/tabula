package uk.ac.warwick.tabula.system

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Closeables._
import org.springframework.beans.factory.BeanInitializationException
import org.hibernate.SessionFactory

/**
 * Selects a Unicode string from the database to see if it gets there and
 * back again in one piece. It doesn't store it anywhere, so it's just a
 * check of the database connection, not whether you're using a Unicode-safe
 * column type like NVARCHAR2.
 *
 * The select query is currently Oracle specific, so this isn't loaded in tests.
 */
class DatabaseEncodingChecker @Autowired() (val sessionFactory: SessionFactory) extends InitializingBean with Logging {

	val testString = "a-\u01ee"

	override def afterPropertiesSet {
		val fetchedString = closeThis(sessionFactory.openSession()) { session =>
			val query = session.createSQLQuery("select :string from dual")
			query.setString("string", testString)
			query.uniqueResult().toString()
		}

		if (testString != fetchedString) {
			throw new BeanInitializationException("Database connection is ruining strings - expected " + testString + ", got " + fetchedString)
		} else {
			logger.debug("Retrieved Unicode string from database in one piece, international characters should be okay")
		}
	}

}