package uk.ac.warwick.tabula.data.model.permissions
import org.reflections.Reflections
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions

class GrantedPermissionPersistenceTest extends PersistenceTestBase {
	
	new Reflections("uk.ac.warwick.tabula").save(getClass.getResource("/").getFile() + "META-INF/reflections/all-reflections.xml")
	
	@Test def saveAndLoad {
		val department = transactional { t =>
			val department = new Department
			department.code = "IN"
			department.name = "IT Services"
				
			session.save(department)
			department
		}
			
		val permission = transactional { t =>
			val permission = GrantedPermission.init(department, Permissions.Department.DownloadFeedbackReport, GrantedPermission.Allow)
			permission.users.addUser("cuscav")
			
			session.save(permission)
			permission
		}
			
		transactional { t =>
			session.load(classOf[GrantedPermission[_]], permission.id) match {
				case permission: GrantedPermission[Department @unchecked] =>
					permission.scope.code should be ("IN")
				case _ => fail("What is this!")
			}
		}
	}

}