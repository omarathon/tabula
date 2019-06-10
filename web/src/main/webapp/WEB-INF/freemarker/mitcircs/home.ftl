<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <h1>Mitigating circumstances</h1>
  <#if panels?has_content>
    <h2>My mitigating circumstances panels</h2>

    <@components.panels_table panels />
  </#if>
  <h2>My department-wide responsibility</h2>
  <#if departments?has_content>
    <ul class="links">
      <#list departments as department>
        <li><a href="<@routes.mitcircs.adminhome department />">Go to the ${department.name} admin page</a></li>
      </#list>
    </ul>
  <#else>
    You do not currently have permission to manage mitigating circumstances. If you think this is incorrect or you need assistance, please visit our <a href="/help">help page</a>.
  </#if>
</#escape>