<#escape x as x?html>
  <#import "*/group_components.ftl" as components />
  <#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

  <h1>Attendance for ${set.module.code?upper_case} ${set.nameWithoutModulePrefix}</h1>

  <@components.single_groupset_attendance set groups />

<#-- List of students modal -->
  <@modal.modal id="students-list-modal"></@modal.modal>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
</#escape>