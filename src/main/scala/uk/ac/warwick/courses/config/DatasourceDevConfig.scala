package uk.ac.warwick.courses.config
import javax.sql.DataSource
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

class DatasourceDevConfig extends DatasourceConfig {
	@Bean override def dataSource() = new EmbeddedDatabaseBuilder()
    		.setType(EmbeddedDatabaseType.HSQL)
    		.addDefaultScripts()
    		.build()
}