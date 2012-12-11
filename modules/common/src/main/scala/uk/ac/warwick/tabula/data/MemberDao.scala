package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.Member
import org.springframework.stereotype.Repository
import org.hibernate.criterion.Restrictions

trait MemberDao {
	def saveOrUpdate(member: Member)
	def getByUniversityId(universityId: String): Option[Member]
}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms {
	import Restrictions._
	
	def saveOrUpdate(member: Member) = session.saveOrUpdate(member)

	def getByUniversityId(universityId: String) = option[Member] {
		session.createQuery("from Member m where universityId = :universityId").setString("universityId", universityId).uniqueResult
	}
}