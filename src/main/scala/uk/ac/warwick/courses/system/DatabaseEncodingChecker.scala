package uk.ac.warwick.courses.system
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.StatementCallback
import javax.sql.DataSource
import uk.ac.warwick.courses.helpers.Logging
import java.sql.Statement
import org.springframework.beans.factory.BeanInitializationException
import org.springframework.orm.hibernate3.HibernateTemplate
import org.springframework.orm.hibernate3.HibernateCallback
import org.hibernate.Session
import org.hibernate.SessionFactory

class DatabaseEncodingChecker @Autowired()(val sessionFactory:SessionFactory) extends InitializingBean with Logging {
  
	val testString = "a-\u01ee"
	val hibernate:HibernateTemplate = new HibernateTemplate(sessionFactory)
	
	override def afterPropertiesSet {
	  logger.info("Checking that string "+testString+" is not ruined by the database (note that logging may not display correctly)")
	  val fetchedString = hibernate.execute(new HibernateCallback[String]{
	    override def doInHibernate(session:Session) = {
	      val query = session.createSQLQuery("select :string from dual")
	      query.setString("string", testString)
	      query.uniqueResult().toString()
	    }
	  })
	  if (!(testString equals fetchedString)) {
	    throw new BeanInitializationException("Database is ruining strings - expected " + testString + ", got "+fetchedString)
	  }
	}
	
}