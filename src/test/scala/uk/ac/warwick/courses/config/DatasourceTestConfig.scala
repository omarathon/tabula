package uk.ac.warwick.courses.config
import org.hibernate.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder

/**
 * This is set to the "test" profile, though that shouldn't be
 * necessary since it's in the test source tree and wouldn't get
 * run with the application unless you packaged the test classes in there.
 */
@Configuration
@Profile(Array("test"))
class DatasourceTestConfig extends DatasourceConfig {
	@Bean def dataSource() = new EmbeddedDatabaseBuilder()
    		.setType(EmbeddedDatabaseType.HSQL)
    		.addDefaultScripts()
    		.build()
}