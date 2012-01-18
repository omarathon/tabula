package uk.ac.warwick.courses.system
import org.springframework.stereotype.Component
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.commands.MigrateBlobsCommand
import org.springframework.transaction.annotation.Transactional

/**
 * This exists to fire off the blob migration command on startup.
 * 
 * TODO remove this code and all references to the BLOB once this
 * is done.
 */
class MigrateBlobs extends InitializingBean with Logging {
    @Transactional
	override def afterPropertiesSet {
		val command = new MigrateBlobsCommand()
		command.apply
		logger.info("Converted %d blobs".format(command.blobsConverted))
	}
}