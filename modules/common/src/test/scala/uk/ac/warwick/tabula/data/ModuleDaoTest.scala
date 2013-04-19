package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Module
import org.junit.Before
import uk.ac.warwick.tabula.data.model.permissions.ModuleGrantedRole
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.data.model.Department

class ModuleDaoTest extends AppContextTestBase {

	@Autowired var dao:ModuleDao =_
	
	var cs108: Module = _
	var cs240: Module = _
	var cs241: Module = _
	
	@Before def setupWithBaseData = transactional { tx => 
		cs108 = dao.getByCode("cs108").get
		cs240 = dao.getByCode("cs240").get
		cs241 = dao.getByCode("cs241").get
	} 
	
	@Test def crud = transactional { tx => 
		dao.allModules should be (Seq(cs108, cs240, cs241))
		
		val cs333 = Fixtures.module("cs333")
		dao.saveOrUpdate(cs333)
		
		dao.allModules should be (Seq(cs108, cs240, cs241, cs333))
		
		dao.getByCode("cs333") should be (Some(cs333))
		dao.getByCode("wibble") should be (None)
		
		dao.getById(cs108.id) should be (Some(cs108))
		dao.getById(cs333.id) should be (Some(cs333))
		dao.getById("wibble") should be (None)
		
		val dept = Fixtures.department("in")
		session.save(dept)
		
		cs333.department = dept
		dao.saveOrUpdate(cs333)
		
		val gr = new ModuleGrantedRole(cs333, ModuleManagerRoleDefinition)
		gr.users.addUser("cuscav")
		gr.users.addUser("cusebr")
		
		session.save(gr)
		session.flush
		
		dao.findByParticipant("cuscav") should be (Seq(cs333))
		dao.findByParticipant("cusebr") should be (Seq(cs333))
		dao.findByParticipant("wibble") should be (Seq())
		
		dao.findByParticipant("cuscav", dept) should be (Seq(cs333))
		dao.findByParticipant("cuscav", session.get(classOf[Department], "1").asInstanceOf[Department]) should be (Seq())
	}
	
}