package uk.ac.warwick.tabula.coursework.commands.assignments

import java.io.FileInputStream
import java.util.zip.ZipInputStream
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.Zips
import uk.ac.warwick.tabula.AppContextTestBase
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.junit.Ignore
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.AnonymousUser
import org.junit.Before
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Department

class DownloadSubmissionsCommandTest extends AppContextTestBase with Mockito {
	
	var userDatabase= Seq(new User())
	var userLookup: UserLookupService = _

	@Before def before {
		userLookup = mock[UserLookupService]
		
		userLookup.getUserByUserId(any[String]) answers { id =>
			userDatabase find {_.getUserId == id} getOrElse (new AnonymousUser())			
		}
		userLookup.getUserByWarwickUniId(any[String]) answers { id =>
			userDatabase find {_.getWarwickId == id} getOrElse (new AnonymousUser())
		}
	}
	
	
	@Test def test {
		val assignment = new Assignment
		assignment.module = new Module(code="ph105", department=new Department)
		
		val cmd = new DownloadSubmissionsCommand(assignment.module, assignment)
		
		cmd.submissions = JArrayList(
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
