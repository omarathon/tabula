package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.TestBase
import org.joda.time.DateTimeConstants
import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.Transactions._

// scalastyle:off magic.number
class ModifyAssignmentCommandTest extends AppContextTestBase with Mockito {
	
	@Test def validateCloseDate {
		// TAB-236
		transactional { t => 
			val f = MyFixtures()
			
			val cmd = new AddAssignmentCommand(f.module)
			val errors = new BindException(cmd, "command")
			
			// No error, close date after open date
			cmd.openDate = new DateTime(2012, DateTimeConstants.JANUARY, 10, 0, 0)
			cmd.closeDate = cmd.openDate.plusDays(1)
			cmd.validate(errors)
			errors.getErrorCount() should be (0)
			
			// Close date is before open date; but open ended so still no error
			cmd.closeDate = cmd.openDate.minusDays(1)
			cmd.openEnded = true
			cmd.validate(errors)
			errors.getErrorCount() should be (0)
			
			// But if we're not open ended 
			cmd.openEnded = false
			cmd.validate(errors)
			errors.getErrorCount() should be (1)		
			withClue("correct error code") { errors.getGlobalError().getCode() should be ("closeDate.early") }
		}
	}
	
	
	case class MyFixtures() {
        val module = Fixtures.module(code="ls101")
        session.save(module)
	}

}