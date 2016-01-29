package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{SHAFileHasherComponent, FileHasherComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.objectstore.{LegacyAwareObjectStorageService, ObjectStorageServiceComponent, AutowiringObjectStorageServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import ObjectStorageMigrationCommand._

object ObjectStorageMigrationCommand {
	type CommandType = Appliable[Seq[String]]

	// The number of keys to transfer per run
	val BatchSize = 100

	def apply(): CommandType =
		new ObjectStorageMigrationCommandInternal
			with ComposableCommand[Seq[String]]
			with ObjectStorageMigrationPermissions
			with AutowiringObjectStorageServiceComponent
			with SHAFileHasherComponent
			with Unaudited with ReadOnly
}

class ObjectStorageMigrationCommandInternal extends CommandInternal[Seq[String]] with Logging {
	self: ObjectStorageServiceComponent with FileHasherComponent =>

	override def applyInternal(): Seq[String] = objectStorageService match {
		case legacyAware: LegacyAwareObjectStorageService =>
			val defaultStore = legacyAware.defaultService
			val legacyStore = legacyAware.legacyService

			legacyStore.listKeys().filterNot(defaultStore.keyExists).take(BatchSize).flatMap { key =>
				for {
					metadata <- legacyStore.metadata(key)
					dataStream <- legacyStore.fetch(key)
					fileHash <- legacyStore.fetch(key).map(fileHasher.hash) // Intentionally a different stream to above
				} yield {
					logger.info(s"Migrating blob (size: ${metadata.contentLength} bytes) for key $key to default store")
					defaultStore.push(key, dataStream, metadata.copy(fileHash = Some(fileHash)))
					key
				}
			}.toList
		case _ =>
			logger.warn("No legacy aware object storage service found - can this be removed?")
			Nil
	}
}

trait ObjectStorageMigrationPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.ReplicaSyncing)
	}
}