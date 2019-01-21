package uk.ac.warwick.tabula

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import org.springframework.beans.factory.{DisposableBean, FactoryBean, InitializingBean}

import scala.annotation.tailrec
import scala.util.{Success, Try}

class PostgreSQLEmbeddedDatabaseFactoryBean extends FactoryBean[DataSource] with InitializingBean with DisposableBean {

	var postgres: EmbeddedPostgres = _

	override def getObjectType: Class[DataSource] = classOf[DataSource]

	override def getObject: DataSource = postgres.getPostgresDatabase

	@tailrec
	private def retry[A](n: Int)(fn: => A): Try[A] =
		Try(fn) match {
			case x: Success[A] => x
			case _ if n > 1 => retry(n - 1)(fn)
			case failure => failure
		}

	override def afterPropertiesSet(): Unit = {
		postgres = retry(3) { // Retry 3 times to do this
			EmbeddedPostgres.builder()
				.setCleanDataDirectory(true)
				// Set parameters for speed over durability
				.setServerConfig("fsync", "false")
				.setServerConfig("synchronous_commit", "off")
				.setServerConfig("full_page_writes", "false")
				.start()
		}.get
	}

	override def destroy(): Unit = {
		if (postgres != null)
			postgres.close()
	}
}
