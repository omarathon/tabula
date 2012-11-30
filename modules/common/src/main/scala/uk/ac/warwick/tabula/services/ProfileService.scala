package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.helpers.{ FoundUser, Logging }
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.spring.Wire

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getMemberByUniversityId(universityId: String): Option[Member]
}

@Service(value = "profileService")
class ProfileServiceImpl extends ProfileService with Logging {
	
	var memberDao = Wire.auto[MemberDao]
	
	def getMemberByUniversityId(universityId: String) = transactional(readOnly = true) {
		memberDao.getByUniversityId(universityId)
	}
	
	def save(member: Member) = memberDao.saveOrUpdate(member)

}