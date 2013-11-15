package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Assignment
import org.hibernate.criterion.{Restrictions, Projections}

trait ExtensionDaoComponent {
	val ExtensionDao: ExtensionDao
}

trait AutowiringExtensionDaoComponent extends ExtensionDaoComponent {
	val ExtensionDao = Wire[ExtensionDao]
}

trait ExtensionDao {
	def getExtensionById(id: String): Option[Extension]
	def saveOrUpdate(extension: Extension)
	def countExtensions(assignment: Assignment): Int
	def countUnapprovedExtensions(assignment: Assignment): Int
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

@Repository
class ExtensionDaoImpl extends ExtensionDao with Daoisms {
	def getExtensionById(id: String) = getById[Extension](id)

	def saveOrUpdate(extension: Extension) = session.saveOrUpdate(extension)

	def countExtensions(assignment: Assignment): Int = {
		session.newCriteria[Extension]
			.add(is("assignment", assignment))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()
	}

	private def unapprovedExtensionsCriteria(assignment: Assignment) = session.newCriteria[Extension]
		.add(is("assignment", assignment))
		.add(
		Restrictions.and(
			Restrictions.isNotNull("requestedOn"),
			Restrictions.eq("approved", false),
			Restrictions.eq("rejected", false)
		)
	)

	def countUnapprovedExtensions(assignment: Assignment): Int = {
		unapprovedExtensionsCriteria(assignment)
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()
	}

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = {
		unapprovedExtensionsCriteria(assignment).seq
	}
}
