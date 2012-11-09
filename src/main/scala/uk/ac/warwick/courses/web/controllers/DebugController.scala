package uk.ac.warwick.courses.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import java.io.Writer
import uk.ac.warwick.courses.data.Transactions._
import uk.ac.warwick.courses.data.Daoisms

@Controller
@RequestMapping(value=Array("/debug"))
class DebugController extends Daoisms {

	// test creating a transaction
	@RequestMapping(value=Array("/count"))
	def transaction(out: Writer) {
		transactional() {
			val count = session.createSQLQuery("select count(*) from assignment").uniqueResult().asInstanceOf[Number]
			out.write("Assignments: " + count)
		}
	}
	
}