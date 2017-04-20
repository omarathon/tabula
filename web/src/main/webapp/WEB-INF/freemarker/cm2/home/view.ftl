<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#-- Do we expect this user to submit assignments, and therefore show them some text even if there aren't any? -->
<#assign expect_assignments = user.student || user.PGR || user.alumni />
<#assign is_marker = false /> <#-- TODO -->
<#assign is_admin = nonempty(moduleManagerDepartments) || nonempty(adminDepartments) />

<#if expect_assignments || !studentInformation.empty>
  <#include "_student.ftl" />
</#if>

<#if is_admin>
  <#include "_admin.ftl" />
</#if>

<#if !expect_assignments && studentInformation.empty && !is_marker && !is_admin>
  <#-- TODO show something if you don't have any permissions at all to avoid blank page c.f. groups -->
</#if>

</#escape>