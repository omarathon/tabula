<#import "../../groups/group_components.ftlh" as group_components />
<#import "*/profiles_macros.ftl" as profiles />
<#escape x as x?html>

  <@profiles.profile_header member isSelf />

  <h1>Small group events</h1>

  <#if hasPermission>

    <div id="student-groups-view">
      <#if commandResult.moduleItems?size == 0>
        <div class="alert alert-info">
          There are no groups to show right now
        </div>
      </#if>
      <@group_components.module_info commandResult />
    </div>

  <#else>

    <div class="alert alert-info">
      You do not have permission to see the seminars for this course.
    </div>

  </#if>



</#escape>
