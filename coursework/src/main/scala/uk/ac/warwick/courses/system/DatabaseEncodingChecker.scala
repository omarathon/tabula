package uk.ac.warwick.courses.system
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.StatementCallback
import javax.sql.DataSource
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.FunctionConversions._
import java.sql.Statement
import org.springframework.beans.factory.BeanInitializationException
import org.springframework.orm.hibernate3.HibernateTemplate
import org.hibernate.Session
import org.hibernate.SessionFactory

/**
 * Selects a Unicode string from the database to see if it gets there and
 * back again in one piece.
 *
 * The select query is currently Oracle specific, so this isn't loaded in tests.
 */
class DatabaseEncodingChecker @Autowired() (val sessionFactory: SessionFactory) extends InitializingBean with Logging {

	val testString = "a-\u01ee"
	val hibernate: HibernateTemplate = new HibernateTemplate(sessionFactory)

	override def afterPropertiesSet {
		val fetchedString = hibernate.execute { session: Session =>
			val query = session.createSQLQuery("select :string from dual")
			query.setString("string", testString)
			query.uniqueResult().toString()
		}
		if (!(testString equals fetchedString)) {
			throw new BeanInitializationException("Database is ruining strings - expected " + testString + ", got " + fetchedString)
		} else {
			logger.debug("Retrieved Unicode string from database in one piece, international characters should be okay")
		}
	}

}