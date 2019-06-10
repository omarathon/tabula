<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <div class="btn-toolbar dept-toolbar">
    <div class="btn-group">
      <a class="btn btn-default" href="<@routes.mitcircs.adminhome department academicYear />">
        Submissions
      </a>
    </div>
  </div>

  <#function route_function dept>
      <#local result><@routes.mitcircs.listPanels dept academicYear /></#local>
      <#return result />
  </#function>
  <@fmt.id7_deptheader "Mitigating Circumstances Panels" route_function "in" />

  <#if panels?has_content>
    <@components.panelsTable panels />
  <#else>
    <p>There are currently no mitigating circumstances panels for ${academicYear.toString}.</p>
  </#if>
</#escape>