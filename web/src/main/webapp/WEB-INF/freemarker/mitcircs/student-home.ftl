<#escape x as x?html>

  <h1 class="with-settings">Personal Circumstances</h1>

  <div class="pull-right">
    <a class="btn btn-primary" href="<@routes.mitcircs.newsubmission student />">Declare mitigating circumstances</a>
  </div>
  <h2>My mitigating circumstances submissions</h2>
  <#if submissions?has_content>
    <table class="table table-condensed">
      <thead>
        <tr>
          <th>Reference</th>
          <th>Issue type</th>
          <th>Start date</th>
          <th>End date</th>
          <th>Last modified</th>
        </tr>
      </thead>
      <tbody>
        <#list submissions as submission>
          <tr>
            <td><a href="<@routes.mitcircs.editsubmission submission />">MIT-${submission.key}</a></td>
            <td>${submission.issueType.description}</td>
            <td><@fmt.date date=submission.startDate includeTime=false /></td>
            <td><@fmt.date date=submission.endDate includeTime=false /></td>
            <td><@fmt.date date=submission.lastModified /></td>
          </tr>
        </#list>
      <tbody>
    </table>
  <#else>
    <p>You have not declared any mitigating circumstances.</p>
  </#if>

</#escape>