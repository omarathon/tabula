package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.{Order, Restrictions}
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model._

trait MemberNoteDaoComponent {
	val memberNoteDao: MemberNoteDao
}

trait AutowiringMemberNoteDaoComponent extends MemberNoteDaoComponent {
	val memberNoteDao = Wire[MemberNoteDao]
}

trait MemberNoteDao {
	def getById(id: String): Option[MemberNote]
	def list(student: Member): Seq[MemberNote]
	def saveOrUpdate(memberNote: MemberNote)

}

@Repository
class MemberNoteDaoImpl extends MemberNoteDao with Daoisms {
	def getById(id: String): Option[MemberNote] = getById[MemberNote](id)

	def list(student: Member): Seq[MemberNote] =
		{
		 session.newCriteria[MemberNote]
			 .add(Restrictions.eq("member", student))
			 .add(Restrictions.disjunction()
			 .add(Restrictions.eq("deleted", false))
		 )
			 .addOrder(Order.desc("lastUpdatedDate"))
			 .seq
	}

	def saveOrUpdate(memberNote: MemberNote) = session.saveOrUpdate(memberNote)
}
