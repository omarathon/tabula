package uk.ac.warwick.tabula.data.model.permissions
import org.reflections.Reflections



import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.Department

class PermissionsTargetUserTypeTest extends PersistenceTestBase {
	
	new Reflections("uk.ac.warwick.tabula").save(getClass.getResource("/").getFile() + "META-INF/reflections/all-reflections.xml")
	
	@Test def saveAndLoad {
		transactional { t =>
			val department = new Department
			department.code = "IN"
			department.name = "IT Services"
				
			session.save(department)
			session.flush
			
			val permission = new GrantedPermission
			permission.users.addUser("cuscav")
			permission.overrideType = permission.Allow
			permission.scope = department
			
			session.save(permission)
			session.flush
			session.clear
			
			session.load(classOf[GrantedPermission], permission.id) match {
				case permission: GrantedPermission =>
					permission.scope should be (department)
				case _ => fail("What is this!")
			}
		}
	}

}