package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

trait MitCircsPanelDaoComponent {
  val mitCircsPanelDao: MitCircsPanelDao
}

trait AutowiringMitCircsPanelDaoComponent extends MitCircsPanelDaoComponent {
  val mitCircsPanelDao: MitCircsPanelDao = Wire[MitCircsPanelDao]
}

trait MitCircsPanelDao {
  def get(id: String): Option[MitigatingCircumstancesPanel]
  def saveOrUpdate(submission: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel
  def getPanels(user: CurrentUser): Set[MitigatingCircumstancesPanel]
}

@Repository
class MitCircsPanelDaoImpl extends MitCircsPanelDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  override def get(id: String): Option[MitigatingCircumstancesPanel] = getById[MitigatingCircumstancesPanel](id)

  override def saveOrUpdate(panel: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel = {
    session.saveOrUpdate(panel)
    panel
  }

  override def getPanels(user: CurrentUser): Set[MitigatingCircumstancesPanel] = {
    // TODO - would be more efficient to return panels from the query directly but I have no idea what the spell for joins on @Any relationships looks like
    // select distinct p from MitigatingCircumstancesPanel p join GrantedRole r on r.scope = p // <- blows up
    session.newQuery[GrantedRole[MitigatingCircumstancesPanel]](s"""
      select distinct r from GrantedRole r
        inner join r._users ug
        left join ug.staticIncludeUsers static on static = :userId
        left join ug.includeUsers include on include = :userId
        left join ug.excludeUsers exclude on exclude = :userId
      where
        r.scopeType = 'MitigatingCircumstancesPanel' and (static is not null or include is not null) and exclude is null
      """)
      .setString("userId", user.apparentId)
      .seq.map(_.scope).toSet
  }

}

