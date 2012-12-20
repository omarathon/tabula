package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.Member
import org.springframework.stereotype.Repository
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import org.joda.time.DateTime
import scala.collection.JavaConversions._

trait MemberDao {
	def saveOrUpdate(member: Member)
	def getByUniversityId(universityId: String): Option[Member]
	def getByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def findByQuery(query: String): Seq[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(member: Member) = session.saveOrUpdate(member)

	def getByUniversityId(universityId: String) = 
		session.newCriteria[Member].add(is("universityId", universityId.trim)).uniqueResult
	
	def getByUserId(userId: String, disableFilter: Boolean = false) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter) 
				session.disableFilter(Member.StudentsOnlyFilter)
				
			session.newCriteria[Member].add(is("userId", userId.trim.toLowerCase)).uniqueResult
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}
	
	def listUpdatedSince(startDate: DateTime, max: Int) = 
		session.newCriteria[Member].add(gt("lastUpdatedDate", startDate)).setMaxResults(max).addOrder(asc("lastUpdatedDate")).list
	
	//TODO
	def findByQuery(query: String) = Seq()
}