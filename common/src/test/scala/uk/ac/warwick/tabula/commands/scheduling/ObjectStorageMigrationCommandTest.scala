package uk.ac.warwick.tabula.commands.scheduling

import java.io.{InputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import com.google.common.io.{ByteSource, CharStreams}
import org.mockito.ArgumentMatcher
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.{FileDao, FileDaoComponent, FileHasherComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.services.objectstore.{LegacyAwareObjectStorageService, ObjectStorageService, ObjectStorageServiceComponent, RichByteSource}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.util.files.hash.FileHasher

class ObjectStorageMigrationCommandTest extends TestBase with Mockito {

	@Test def noLegacyStore(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with SHAFileHasherComponent with ObjectStorageServiceComponent with FileDaoComponent {
			val objectStorageService: ObjectStorageService = smartMock[ObjectStorageService]
			val fileDao: FileDao = smartMock[FileDao]
		}

		command.applyInternal() should be ('empty)
	}

	private trait CommandTestSupport extends FileHasherComponent with ObjectStorageServiceComponent with FileDaoComponent {
		val defaultStoreService: ObjectStorageService = smartMock[ObjectStorageService]
		val legacyStoreService: ObjectStorageService = smartMock[ObjectStorageService]

		val objectStorageService = new LegacyAwareObjectStorageService(
			defaultService = defaultStoreService,
			legacyService = legacyStoreService
		)

		val fileDao: FileDao = smartMock[FileDao]
		val fileHasher: FileHasher = smartMock[FileHasher]
	}

	@Test def transfer(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with CommandTestSupport

		when(command.fileDao.getAllFileIds(None)) thenReturn Set("1", "2", "3", "4", "5", "6", "7", "8", "9")
		when(command.defaultStoreService.listKeys()) thenReturn Set("1", "2", "13", "4", "15", "16", "7", "18", "19").toStream

		val blob3is = smartMock[InputStream]
		val blob5is = smartMock[InputStream]
		val blob6is = smartMock[InputStream]
		val blob8is = smartMock[InputStream]
		val blob9is = smartMock[InputStream]

		val blob3data = smartMock[ByteSource]
		val blob5data = smartMock[ByteSource]
		val blob6data = smartMock[ByteSource]
		val blob8data = smartMock[ByteSource]
		val blob9data = smartMock[ByteSource]

		when(blob3data.openStream()) thenReturn blob3is
		when(blob5data.openStream()) thenReturn blob5is
		when(blob6data.openStream()) thenReturn blob6is
		when(blob8data.openStream()) thenReturn blob8is
		when(blob9data.openStream()) thenReturn blob9is

		val metadata3 = ObjectStorageService.Metadata(3, "application/custom-3", None)
		val metadata5 = ObjectStorageService.Metadata(5, "application/custom-5", None)
		val metadata6 = ObjectStorageService.Metadata(6, "application/custom-6", None)
		val metadata8 = ObjectStorageService.Metadata(8, "application/custom-8", None)
		val metadata9 = ObjectStorageService.Metadata(9, "application/custom-9", None)

		when(command.legacyStoreService.fetch("3")) thenReturn RichByteSource.wrap(blob3data, Some(metadata3))
		when(command.legacyStoreService.fetch("5")) thenReturn RichByteSource.wrap(blob5data, Some(metadata5))
		when(command.legacyStoreService.fetch("6")) thenReturn RichByteSource.wrap(blob6data, Some(metadata6))
		when(command.legacyStoreService.fetch("8")) thenReturn RichByteSource.wrap(blob8data, Some(metadata8))
		when(command.legacyStoreService.fetch("9")) thenReturn RichByteSource.wrap(blob9data, Some(metadata9))

		when(command.fileHasher.hash(blob3is)) thenReturn "hash3"
		when(command.fileHasher.hash(blob5is)) thenReturn "hash5"
		when(command.fileHasher.hash(blob6is)) thenReturn "hash6"
		when(command.fileHasher.hash(blob8is)) thenReturn "hash8"
		when(command.fileHasher.hash(blob9is)) thenReturn "hash9"

		command.applyInternal() should be (Set("3", "5", "6", "8", "9"))

		verify(command.defaultStoreService, times(1)).push(isEq("3"), any[RichByteSource], isEq(metadata3.copy(fileHash = Some("hash3"))))
		verify(command.defaultStoreService, times(1)).push(isEq("5"), any[RichByteSource], isEq(metadata5.copy(fileHash = Some("hash5"))))
		verify(command.defaultStoreService, times(1)).push(isEq("6"), any[RichByteSource], isEq(metadata6.copy(fileHash = Some("hash6"))))
		verify(command.defaultStoreService, times(1)).push(isEq("8"), any[RichByteSource], isEq(metadata8.copy(fileHash = Some("hash8"))))
		verify(command.defaultStoreService, times(1)).push(isEq("9"), any[RichByteSource], isEq(metadata9.copy(fileHash = Some("hash9"))))
	}

}
