package uk.ac.warwick.tabula.coursework.commands.assignments

import java.io.FileInputStream
import java.util.zip.ZipInputStream
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.coursework.data.model.Submission
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.coursework.services.Zips
import uk.ac.warwick.tabula.coursework.AppContextTestBase
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.data.model.FileAttachment


class DownloadSubmissionsCommandTest extends AppContextTestBase {
	@Test def test {
		val cmd = new DownloadSubmissionsCommand
		cmd.assignment = new Assignment
		cmd.submissions = ArrayList(
			newSubmission(cmd.assignment),
			newSubmission(cmd.assignment),
			newSubmission(cmd.assignment)
		)
		cmd.apply { zip =>
			val stream = new ZipInputStream(new FileInputStream(zip.file.get))
			val items = Zips.map(stream) { item =>
				item.getName
			}
			items.size should be (0)
		}
	}
	
	def newSubmission(a:Assignment, values:JSet[SavedSubmissionValue]=null) = {
		val s = new Submission
		s.assignment = a
		if (values != null) s.values = values
		s
	}
}
