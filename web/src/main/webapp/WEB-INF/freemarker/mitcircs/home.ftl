<#escape x as x?html>
    <h1>Mitigating circumstances</h1>
    <#if panels?has_content>
      <h2>My mitigating circumstances panels</h2>
      <table class="table table-condensed">
        <thead>
          <tr>
            <th>Name</th>
            <th>Date</th>
            <th>Location</th>
            <th>Chair</th>
            <th>Secretary</th>
            <th>Members</th>
            <th>Submissions</th>
          </tr>
        </thead>
        <#list panels as panel>
          <tbody>
            <tr>
              <td>${panel.name}</td>
              <td>
                <@fmt.date date=panel.date includeTime=false relative=false />: <@fmt.time panel.startTime /> &mdash; <@fmt.time panel.endTime />
              </td>
              <td><#if panel.location??><@fmt.location panel.location /></#if></td>
              <td><#if panel.chair??>${panel.chair.fullName}</#if></td>
              <td><#if panel.secretary??>${panel.secretary.fullName}</#if></td>
              <td><#list panel.members as member>${member.fullName}<#if member_has_next>, </#if></#list></td>
              <td>${panel.submissions?size}</td>
            </tr>
          </tbody>
        </#list>
      </table>
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