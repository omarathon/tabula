package uk.ac.warwick.tabula.data
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Order, Projections}
import org.joda.time.DateTime.now
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Assignment, Module}

trait ModuleDao {
	def allModules: Seq[Module]
	def saveOrUpdate(module: Module)
	def getByCode(code: String): Option[Module]
	def getById(id: String): Option[Module]
	def stampMissingFromImport(newStaleModuleCodes: Seq[String])
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

	def stampMissingFromImport(staleModuleCodes: Seq[String]) = {
		staleModuleCodes.grouped(Daoisms.MaxInClauseCount).foreach { staleCodes =>
			val sqlString = """
				update
					Module
				set
					missingFromImportSince = :now
				where
					code in (:staleModuleCodes)
		 		and
		 			missingFromImportSince is null
			"""

			session.newQuery(sqlString)
				.setParameter("now", now)
				.setParameterList("staleModuleCodes", staleCodes)
				.executeUpdate()
		}
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