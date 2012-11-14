package uk.ac.warwick.tabula.coursework.services
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.helpers.Logging
import javax.sql.DataSource
import javax.annotation.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.`object`.MappingSqlQuery
import java.sql.ResultSet
import collection.JavaConversions._
import org.springframework.jdbc.core.SqlParameter
import java.sql.Types
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters

case class DepartmentInfo(val name: String, val code: String, val faculty: String)
case class ModuleInfo(val name: String, val code: String, val group: String)

/**
 * Retrieves department and module information from an external location.
 */
trait ModuleImporter {
	def getModules(deptCode: String): Seq[ModuleInfo]
	def getDepartments: Seq[DepartmentInfo]
}

/**
 * Retrieves department and module information from Webgroups.
 */
@Service
class ModuleImporterImpl extends ModuleImporter with Logging {
	import ModuleImporter._

	@Resource(name = "academicDataStore") var ads: DataSource = _

	lazy val departmentInfoMappingQuery = new DepartmentInfoMappingQuery(ads)
	lazy val moduleInfoMappingQuery = new ModuleInfoMappingQuery(ads)

	def getDepartments: Seq[DepartmentInfo] = {
		departmentInfoMappingQuery.execute()
	}

	def getModules(deptCode: String): Seq[ModuleInfo] = {
		val result = moduleInfoMappingQuery.execute(deptCode.toUpperCase)
		result
	}
}

object ModuleImporter {

	final val GetDepartmentsSql = 
		"""select d.name name, department_code code, f.name faculty from department d
			join faculty f on f.faculty_code = d.faculty_code"""
	final val GetModulesSql = 
        """select xcode code, name from (
          select substr(module_code,0,5) as xcode, max(modified_date) maxmod from module x
                      where module_code like '_____-%'
                      and department_code = ?
                      and in_use = 'Y'
                      group by substr(module_code,0,5)
        ) x inner join module m on substr(m.module_code,0,5) = xcode and m.modified_date = maxmod"""


	class DepartmentInfoMappingQuery(ds: DataSource) extends MappingSqlQuery[DepartmentInfo](ds, GetDepartmentsSql) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) =
			DepartmentInfo(
				rs.getString("name"),
				rs.getString("code").toLowerCase(),
				rs.getString("faculty"))
	}

	class ModuleInfoMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[ModuleInfo](ds, GetModulesSql) {
		declareParameter(new SqlParameter("dept", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: java.util.Map[_, _]) = {
			val moduleCode = rs.getString("code").toLowerCase
			val deptCode = params(0).toString.toLowerCase
			ModuleInfo(
				rs.getString("name"),
				moduleCode,
				deptCode + "-" + moduleCode)
		}
	}

}
