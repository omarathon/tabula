<#escape x as x?html>
  <#if departments?has_content>
    <h1>Mitigating circumstances</h1>
    <h6>My department-wide responsibility</h6>
    <ul class="links">
      <#list departments as department>
        <li><a href="<@routes.mitcircs.adminhome department />">Go to the ${department.name} admin page</a></li>
      </#list>
    </ul>
  <#else>
    You do not currently have permission to view or manage any monitoring points. If you think this is incorrect or you need assistance, please visit our <a href="/help">help page</a>.
  </#if>
</#escape>