package uk.ac.warwick.courses.config

import java.lang.Object
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder
import org.springframework.orm.hibernate3.HibernateTransactionManager
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.model._
import org.springframework.context.annotation.ComponentScan

@Configuration
@ComponentScan(Array("uk.ac.warwick.courses.data"))
class Persistence extends Object with Logging {

	@Autowired var dataSourceConfig:DatasourceConfig = null
  
    @Bean def sessionFactory:SessionFactory = {
    	new AnnotationSessionFactoryBuilder()
			.setDataSource(dataSourceConfig.dataSource)
			.setAnnotatedClasses(
			    classOf[Module],
			    classOf[Department],
			    classOf[Assignment]
			)
			//.setAnnotatedPackages("uk.ac.warwick.courses.data.model")
			.buildSessionFactory()
    }
	
	@Bean def transactionManager:HibernateTransactionManager = new HibernateTransactionManager {
		setSessionFactory(sessionFactory)
	}
    
}