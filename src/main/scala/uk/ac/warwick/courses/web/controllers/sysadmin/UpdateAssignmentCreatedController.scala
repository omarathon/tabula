package uk.ac.warwick.courses.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.controllers.BaseController
import java.io.Writer
import uk.ac.warwick.courses.services.AuditEventIndexService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.Daoisms
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Temporary controller to update assignments that don't have created dates with the date
 * found in the index.
 *
 * Assignments created after this deploy will have the value provided so we can get rid
 * of this controller after it's been used. Which is why the below is so very hacky!
 */
@Controller
@RequestMapping(value=Array("/sysadmin/temp/assignmentdates"))
class UpdateAssignmentCreatedController extends BaseController with Daoisms {

	@Autowired var indexer:AuditEventIndexService =_

	//yay, inline XML!!!
	@RequestMapping(method=Array(GET))
	def form(writer:Writer) {
		writer.write(
		<html>
			<head>
				<title>Fix up assignment creation dates</title>
			</head>
			<body>
				<form method="POST" action="?yeah">
					Go <input type="submit"/>
				</form>
			</body>
		</html>
		.toString())
	}

	// find all the dateless assignments and DATE THEM UP
	@Transactional
	@RequestMapping(method=Array(POST))
	def submit() {
		for(assignment <- getDatelessAssignments();
			date <- indexer.getAssignmentCreatedDate(assignment)) {
			assignment.createdDate = date
			session.saveOrUpdate(assignment)
		}
	}

	def getDatelessAssignments() = {
		session.newCriteria[Assignment]
		  .add(Restrictions.isNull("createdDate"))
		  .seq
	}
}
