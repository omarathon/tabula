package uk.ac.warwick.tabula.coursework.commands.assignments

import java.io.FileInputStream
import java.util.zip.ZipInputStream

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, Submission}
import uk.ac.warwick.tabula.services.{UserLookupService, ZipService, Zips}
import uk.ac.warwick.tabula.{Features, Mockito, TestBase}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

class DownloadSubmissionsCommandTest extends TestBase with Mockito {

	var userDatabase = Seq(new User())
	var userLookup: UserLookupService = smartMock[UserLookupService]
	userLookup.getUserByUserId(any[String]) answers { id =>
		userDatabase find {_.getUserId == id} getOrElse new AnonymousUser()
	}
	userLookup.getUserByWarwickUniId(any[String]) answers { id =>
		userDatabase find {_.getWarwickId == id} getOrElse new AnonymousUser()
	}

	@Test def test() {
		val assignment = new Assignment
		assignment.module = new Module(code = "ph105", adminDepartment = new Department)

		val cmd = new DownloadSubmissionsCommand(assignment.module, assignment)

		val submissions = JArrayList(
			newSubmission(cmd.assignment),
			newSubmission(cmd.assignment),
			newSubmission(cmd.assignment)
		)

		cmd.zipService = new ZipService
		cmd.zipService.userLookup = userLookup
		cmd.zipService.features = Features.empty
		cmd.zipService.zipDir = createTemporaryDirectory()

		cmd.submissions = submissions

		cmd.callback = { zip =>
			val stream = new ZipInputStream(new FileInputStream(zip.file.get))
			val items = Zips.map(stream) { item =>
				item.getName
			}
			items.size should be (0)
		}

		cmd.applyInternal()
	}


	private def newSubmission(a: Assignment, values: JSet[SavedFormValue] = null) = {
		val s = new Submission
		s.assignment = a
		if (values != null) s.values = values
		s
	}
}
