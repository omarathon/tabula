<#escape x as x?html>
  <#import "*/group_components.ftl" as components />
  <#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

  <h1>
    ${group.groupSet.module.code?upper_case}<span class="hide-smallscreen"> ${group.groupSet.nameWithoutModulePrefix}</span>, ${group.name}
    <#if can.do("SmallGroups.ReadMembership", group)>
      <a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
        <small><@fmt.p (group.students.size)!0 "student" "students" /></small>
      </a>
    <#else>
      <small><@fmt.p (group.students.size)!0 "student" "students" /></small>
    </#if>
  </h1>

  <ul class="unstyled margin-fix">
    <#list group.events as event>
      <li>
        <#if event.tutors??>
          <h6>Tutor<#if (event.tutors.size > 1)>s</#if>:
            <#if (event.tutors.size < 1)>[no tutor]</#if>
            <#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
          </h6>
        </#if>
        <@components.event_schedule_info event />
      </li>
    </#list>
  </ul>

  <@components.singleGroupAttendance group instances studentAttendance attendanceNotes />

<#-- List of students modal -->
  <@modal.modal id="students-list-modal"></@modal.modal>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
</#escape>