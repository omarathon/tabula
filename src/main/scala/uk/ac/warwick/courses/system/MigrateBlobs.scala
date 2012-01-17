package uk.ac.warwick.courses.system
import org.springframework.stereotype.Component
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.commands.MigrateBlobsCommand

/**
 * This exists to fire off the blob migration command on startup.
 */
@Component
class MigrateBlobs extends InitializingBean with Logging {
	override def afterPropertiesSet {
		new MigrateBlobsCommand().apply
	}
}