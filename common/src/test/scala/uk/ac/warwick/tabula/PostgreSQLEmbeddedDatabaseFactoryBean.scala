package uk.ac.warwick.tabula

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import org.springframework.beans.factory.{DisposableBean, FactoryBean, InitializingBean}

class PostgreSQLEmbeddedDatabaseFactoryBean extends FactoryBean[DataSource] with InitializingBean with DisposableBean {

	var postgres: EmbeddedPostgres = _

	override def getObjectType: Class[DataSource] = classOf[DataSource]

	override def getObject: DataSource = postgres.getPostgresDatabase

	override def afterPropertiesSet(): Unit = {
		postgres = EmbeddedPostgres.builder()
			.setCleanDataDirectory(true)
			// Set parameters for speed over durability
			.setServerConfig("fsync", "false")
			.setServerConfig("synchronous_commit", "off")
			.setServerConfig("full_page_writes", "false")
			.start()
	}

	override def destroy(): Unit = {
		if (postgres != null)
			postgres.close()
	}
}
