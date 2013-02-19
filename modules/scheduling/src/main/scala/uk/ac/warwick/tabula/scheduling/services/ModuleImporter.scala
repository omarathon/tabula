package uk.ac.warwick.tabula.scheduling.services
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.helpers.Logging
import javax.sql.DataSource
import javax.annotation.Resource
import org.springframework.jdbc.`object`.MappingSqlQuery
import java.sql.ResultSet
import collection.JavaConversions._
import org.springframework.jdbc.core.SqlParameter
import java.sql.Types
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import uk.ac.warwick.tabula.data.model.DegreeType
import javax.annotation.Resource
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire

case class DepartmentInfo(val name: String, val code: String, val faculty: String)
case class ModuleInfo(val name: String, val code: String, val group: String)
case class RouteInfo(val name: String, val code: String, val degreeType: DegreeType)

/**
 * Retrieves department and module information from an external location.
 */
trait ModuleImporter {
	def getModules(deptCode: String): Seq[ModuleInfo]
	def getRoutes(deptCode: String): Seq[RouteInfo]
	def getDepartments: Seq[DepartmentInfo]
}

/**
 * Retrieves department and module information from Webgroups.
 */
@Service
class ModuleImporterImpl extends ModuleImporter with Logging {
	import ModuleImporter._

	var ads = Wire[DataSource]("academicDataStore")
	var sits = Wire[DataSource]("sitsDataSource")
	var membership = Wire[DataSource]("membershipDataSource")

	lazy val departmentInfoMappingQuery = new DepartmentInfoMappingQuery(membership)
	lazy val moduleInfoMappingQuery = new ModuleInfoMappingQuery(ads)
	lazy val routeInfoMappingQuery = new RouteInfoMappingQuery(ads)

	def getDepartments: Seq[DepartmentInfo] = departmentInfoMappingQuery.execute
	def getModules(deptCode: String): Seq[ModuleInfo] = moduleInfoMappingQuery.execute(deptCode.toUpperCase)
	def getRoutes(deptCode: String): Seq[RouteInfo] = routeInfoMappingQuery.execute(deptCode.toUpperCase)
}

object ModuleImporter {

	final val GetDepartmentsSql = """
		select 
			d.department_name name, 
			d.department_code code,
			f.faculty_name faculty
  
		from cmsowner.uow_departments d 
			join cmsowner.uow_faculties f
				on d.faculty_code = f.faculty_code
      
		where d.department_code is not null
		"""
	final val GetModulesSql = 
        """select xcode code, name from (
          select substr(module_code,0,5) as xcode, max(modified_date) maxmod from module x
                      where module_code like '_____-%'
                      and case when delivered_dept_code is null then department_code
                        else delivered_dept_code
                        end = ?
                      and in_use = 'Y'
                      group by substr(module_code,0,5)
        ) x inner join module m on substr(m.module_code,0,5) = xcode and m.modified_date = maxmod"""
	final val GetRoutesSql =
	  	"""select r.route_code as code, r.name, r.degree_type from route r where r.department_code = ? and r.in_use = 'Y'"""


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
	
	class RouteInfoMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[RouteInfo](ds, GetRoutesSql) {
		declareParameter(new SqlParameter("dept", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: java.util.Map[_, _]) = {
			val routeCode = rs.getString("code").toLowerCase
			RouteInfo(
				rs.getString("name"),
				routeCode,
				DegreeType.fromCode(rs.getString("degree_type")))
		}
	}

}
