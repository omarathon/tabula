<h1>Marks management</h1>

<#macro link_to_department department>
  <a href="<@routes.marks.adminhome department academicYear />">
    ${department.name}
  </a>
</#macro>

<#if nonempty(moduleManagerDepartments)>
  <h2>My managed modules</h2>

  <ul>
    <#list moduleManagerDepartments as department>
      <li><@link_to_department department /></li>
    </#list>
  </ul>
</#if>

<#if nonempty(adminDepartments)>
  <h2>My department-wide responsibilities</h2>

  <ul>
    <#list adminDepartments as department>
      <li><@link_to_department department /></li>
    </#list>
  </ul>
</#if>
