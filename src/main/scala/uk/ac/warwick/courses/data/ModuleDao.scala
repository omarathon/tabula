package uk.ac.warwick.courses.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import collection.JavaConverters._
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model.Department

trait ModuleDao {
	def saveOrUpdate(module: Module)
	def getByCode(code: String): Option[Module]
	def findByParticipant(userId: String): Seq[Module]
	def findByParticipant(userId: String, dept: Department): Seq[Module]
}

@Repository
class ModuleDaoImpl extends ModuleDao with Daoisms {

	def saveOrUpdate(module: Module) = session.saveOrUpdate(module)

	def getByCode(code: String) = option[Module] {
		session.createQuery("from Module m where code = :code").setString("code", code).uniqueResult
	}

	def findByParticipant(userId: String): Seq[Module] = {
		session.createQuery("""select m from Module m 
	 		  left join m.participants as p
	 		  where :user in elements(p.includeUsers)
	 		  """)
			.setString("user", userId)
			.list().asInstanceOf[JList[Module]].asScala.toSeq
	}

	/**
	 * Find modules managed by this user, in this department.
	 */
	def findByParticipant(userId: String, dept: Department): Seq[Module] = {
		session.createQuery("""select m from Module m 
	 		  join m.department as d
	 		  left join m.participants as p
	 		  where d = :department and :user in elements(p.includeUsers)
	 		  """)
			.setString("user", userId)
			.setEntity("department", dept)
			.list().asInstanceOf[JList[Module]].asScala.toSeq
	}

}