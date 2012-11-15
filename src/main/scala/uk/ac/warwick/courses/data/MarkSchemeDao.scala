package uk.ac.warwick.courses.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model._
import org.hibernate.criterion.Restrictions

trait MarkSchemeDao {
	
	/** All assignments using this mark scheme. */
	def getAssignmentsUsingMarkScheme(markScheme: MarkScheme): JList[Assignment]

}

@Repository
class MarkSchemeDaoImpl extends MarkSchemeDao with Daoisms {
	
	def getAssignmentsUsingMarkScheme(markScheme: MarkScheme): JList[Assignment] = 
		session.newCriteria[Assignment]
			.add(Restrictions.eq("markScheme", markScheme))
			.list

}