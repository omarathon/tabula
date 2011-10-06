package uk.ac.warwick.courses.config
import javax.sql.DataSource
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Configuration
import org.springframework.jndi.JndiObjectFactoryBean
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.beans.factory.annotation.Value

@Configuration
@Profile(Array("dev", "production"))
class DatasourceLiveConfig extends DatasourceConfig {
  
    @Value("${courses.datasource.password}")var dsPassword=""
  
	@Bean override def dataSource:DataSource = { 
	  val ds = new BasicDataSource 
	  ds.setDriverClassName("oracle.jdbc.OracleDriver")
	  ds.setUrl("jdbc:oracle:thin://@dev-db-host:1666/dev-db.warwick.ac.uk")
	  ds.setUsername("courses")
	  ds.setPassword(dsPassword)
	  ds
	}  
	
}