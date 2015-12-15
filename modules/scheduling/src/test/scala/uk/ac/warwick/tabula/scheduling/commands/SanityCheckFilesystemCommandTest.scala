package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.{FileDaoComponent, FileDao}
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.springframework.util.FileCopyUtils
import java.io.FileReader
import uk.ac.warwick.tabula.services.MaintenanceModeServiceImpl

// scalastyle:off magic.number
class SanityCheckFilesystemCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends FileDaoComponent {
		override val fileDao = mock[FileDao]
	}

	val cmd = new SanityCheckFilesystemCommand with CommandTestSupport
	cmd.fileSyncEnabled = false
	cmd.dataDir = createTemporaryDirectory().getAbsolutePath

	// Don't bother about updating hashes for the moment
	cmd.maintenanceModeService = new MaintenanceModeServiceImpl
	cmd.maintenanceModeService.enable

	// The fake time is just to make sure the log string is consistent
	@Test def itWorks(): Unit = withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 13, 15, 18, 2, 278)) {
		cmd.fileDao.getAllFileIds(None) returns Set("file1", "file2", "file3", "file4", "file5")

		val file = new FileAttachment

		cmd.fileDao.getFileById("file1") returns Some(file)
		cmd.fileDao.getFileById("file2") returns Some(file)
		cmd.fileDao.getFileById("file3") returns Some(file)
		cmd.fileDao.getFileById("file4") returns Some(file)
		cmd.fileDao.getFileById("file5") returns Some(file)

		// file1 and file2 are found and fine
		// file3 isn't found, but it's since been cleaned up so it's not a failure
		// file4 and file5 are missing
		cmd.fileDao.getData("file1") returns Some(createTemporaryFile())
		cmd.fileDao.getData("file2") returns Some(createTemporaryFile())
		cmd.fileDao.getData("file3") returns None
		cmd.fileDao.getData("file4") returns None
		cmd.fileDao.getData("file5") returns None

		cmd.fileDao.getFileById("file3") returns None
		cmd.fileDao.getFileById("file4") returns Some(new FileAttachment)
		cmd.fileDao.getFileById("file5") returns Some(new FileAttachment)

		cmd.applyInternal()

		// Read the log string
		val output = FileCopyUtils.copyToString(new FileReader(cmd.lastSanityCheckJobDetailsFile)).trim
		output should be ("successfulFiles,2,failedFiles,2,timeTaken,0,lastSuccessfulRun,1358090282278")
	}

}