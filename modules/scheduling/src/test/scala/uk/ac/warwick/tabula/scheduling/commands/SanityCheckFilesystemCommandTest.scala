package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.springframework.util.FileCopyUtils
import java.io.FileReader

// scalastyle:off magic.number
class SanityCheckFilesystemCommandTest extends TestBase with Mockito {
	
	val dao = mock[FileDao]
	
	val cmd = new SanityCheckFilesystemCommand
	cmd.fileDao = dao
	cmd.fileSyncEnabled = false
	cmd.dataDir = createTemporaryDirectory.getAbsolutePath
	
	// The fake time is just to make sure the log string is consistent
	@Test def itWorks = withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 13, 15, 18, 2, 278)) {
		dao.getAllFileIds(None) returns (Set("file1", "file2", "file3", "file4", "file5"))
		
		// file1 and file2 are found and fine
		// file3 isn't found, but it's since been cleaned up so it's not a failure
		// file4 and file5 are missing
		dao.getData("file1") returns (Some(createTemporaryFile))
		dao.getData("file2") returns (Some(createTemporaryFile))
		dao.getData("file3") returns (None)
		dao.getData("file4") returns (None)
		dao.getData("file5") returns (None)
		
		dao.getFileById("file3") returns (None)
		dao.getFileById("file4") returns (Some(new FileAttachment))
		dao.getFileById("file5") returns (Some(new FileAttachment))
		
		cmd.applyInternal()
		
		// Read the log string
		val output = FileCopyUtils.copyToString(new FileReader(cmd.lastSanityCheckJobDetailsFile)).trim
		output should be ("successfulFiles,2,failedFiles,2,timeTaken,0,lastSuccessfulRun,1358090282278")
	}

}