package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Department}
import org.hibernate.criterion.{Projections, Order}
import org.joda.time.DateTime
import org.hibernate.criterion.Restrictions._

trait ModuleDao {
	def allModules: Seq[Module]
	def saveOrUpdate(module: Module)
	def getByCode(code: String): Option[Module]
	def getById(id: String): Option[Module]
	def stampMissingRows(dept: Department, seenCodes: Seq[String]): Int
	def hasAssignments(module: Module): Boolean
	def findModulesNamedLike(query: String): Seq[Module]

}

@Repository
class ModuleDaoImpl extends ModuleDao with Daoisms {

	def allModules: Seq[Module] =
		session.newCriteria[Module]
			.addOrder(Order.asc("code"))
			.seq
			.distinct

	def saveOrUpdate(module: Module) = session.saveOrUpdate(module)

	def getByCode(code: String) = 
		session.newQuery[Module]("from Module m where code = :code").setString("code", code).uniqueResult
	
	def getById(id: String) = getById[Module](id)
	
	def stampMissingRows(dept: Department, seenCodes: Seq[String]) = {
		val hql = """
				update Module m
				set
					m.missingFromImportSince = :now
				where
					m.department = :department and
					m.missingFromImportSince is null
		"""
		
		val query = 
			if (seenCodes.isEmpty) session.newQuery(hql)
			else session.newQuery(hql + " and m.code not in (:seenCodes)").setParameterList("seenCodes", seenCodes)
		 
		query
			.setParameter("now", DateTime.now)
			.setEntity("department", dept)
			.executeUpdate()
	}

	def hasAssignments(module: Module): Boolean = {
		session.newCriteria[Assignment]
			.add(is("module", module))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue() > 0
	}

	def findModulesNamedLike(query: String): Seq[Module] = {
		session.newCriteria[Module]
			.add(disjunction()
			.add(like("code", s"%${query.toLowerCase}%").ignoreCase)
			.add(like("name", s"%${query.toLowerCase}%").ignoreCase)
			)
			.addOrder(Order.asc("code"))
			.setMaxResults(20).seq
	}

}