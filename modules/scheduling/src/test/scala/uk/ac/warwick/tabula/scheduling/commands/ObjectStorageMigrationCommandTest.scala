package uk.ac.warwick.tabula.scheduling.commands

import java.io.InputStream

import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.{FileHasherComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.services.objectstore.{ObjectStorageServiceComponent, LegacyAwareObjectStorageService, ObjectStorageService}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.util.files.hash.FileHasher

class ObjectStorageMigrationCommandTest extends TestBase with Mockito {

	@Test def noLegacyStore(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with SHAFileHasherComponent with ObjectStorageServiceComponent {
			val objectStorageService = smartMock[ObjectStorageService]
		}

		command.applyInternal() should be ('empty)
	}

	private trait CommandTestSupport extends FileHasherComponent with ObjectStorageServiceComponent {
		val defaultStoreService = smartMock[ObjectStorageService]
		val legacyStoreService = smartMock[ObjectStorageService]

		val objectStorageService = new LegacyAwareObjectStorageService(
			defaultService = defaultStoreService,
			legacyService = legacyStoreService
		)

		val fileHasher = smartMock[FileHasher]
	}

	@Test def transfer(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with CommandTestSupport

		when(command.legacyStoreService.listKeys()) thenReturn Stream("1", "2", "3", "4", "5", "6", "7", "8", "9")
		when(command.defaultStoreService.keyExists("1")) thenReturn {true}
		when(command.defaultStoreService.keyExists("2")) thenReturn {true}
		when(command.defaultStoreService.keyExists("3")) thenReturn {false}
		when(command.defaultStoreService.keyExists("4")) thenReturn {true}
		when(command.defaultStoreService.keyExists("5")) thenReturn {false}
		when(command.defaultStoreService.keyExists("6")) thenReturn {false}
		when(command.defaultStoreService.keyExists("7")) thenReturn {true}
		when(command.defaultStoreService.keyExists("8")) thenReturn {false}
		when(command.defaultStoreService.keyExists("9")) thenReturn {false}

		val blob3data = mock[InputStream]
		val blob5data = mock[InputStream]
		val blob6data = mock[InputStream]
		val blob8data = mock[InputStream]
		val blob9data = mock[InputStream]

		val metadata3 = ObjectStorageService.Metadata(3, "application/custom-3", None)
		val metadata5 = ObjectStorageService.Metadata(5, "application/custom-5", None)
		val metadata6 = ObjectStorageService.Metadata(6, "application/custom-6", None)
		val metadata8 = ObjectStorageService.Metadata(8, "application/custom-8", None)
		val metadata9 = ObjectStorageService.Metadata(9, "application/custom-9", None)

		when(command.legacyStoreService.fetch("3")) thenReturn Some(blob3data)
		when(command.legacyStoreService.fetch("5")) thenReturn Some(blob5data)
		when(command.legacyStoreService.fetch("6")) thenReturn Some(blob6data)
		when(command.legacyStoreService.fetch("8")) thenReturn Some(blob8data)
		when(command.legacyStoreService.fetch("9")) thenReturn Some(blob9data)

		when(command.legacyStoreService.metadata("3")) thenReturn Some(metadata3)
		when(command.legacyStoreService.metadata("5")) thenReturn Some(metadata5)
		when(command.legacyStoreService.metadata("6")) thenReturn Some(metadata6)
		when(command.legacyStoreService.metadata("8")) thenReturn Some(metadata8)
		when(command.legacyStoreService.metadata("9")) thenReturn Some(metadata9)

		when(command.fileHasher.hash(blob3data)) thenReturn "hash3"
		when(command.fileHasher.hash(blob5data)) thenReturn "hash5"
		when(command.fileHasher.hash(blob6data)) thenReturn "hash6"
		when(command.fileHasher.hash(blob8data)) thenReturn "hash8"
		when(command.fileHasher.hash(blob9data)) thenReturn "hash9"

		command.applyInternal() should be (Seq("3", "5", "6", "8", "9"))

		verify(command.defaultStoreService, times(1)).push("3", blob3data, metadata3.copy(fileHash = Some("hash3")))
		verify(command.defaultStoreService, times(1)).push("5", blob5data, metadata5.copy(fileHash = Some("hash5")))
		verify(command.defaultStoreService, times(1)).push("6", blob6data, metadata6.copy(fileHash = Some("hash6")))
		verify(command.defaultStoreService, times(1)).push("8", blob8data, metadata8.copy(fileHash = Some("hash8")))
		verify(command.defaultStoreService, times(1)).push("9", blob9data, metadata9.copy(fileHash = Some("hash9")))
	}

}
