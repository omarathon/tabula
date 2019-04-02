<#escape x as x?html>
  <div class="pull-right">
    <a class="btn btn-primary" href="<@routes.mitcircs.newsubmission student />">Declare mitigating circumstances</a>
  </div>
  <h1 class="with-settings">My Mitigating Circumstances Submissions</h1>

  <#if submissions?has_content>
    <table class="table table-condensed">
      <thead>
        <tr>
          <th></th>
          <th>Issue type</th>
          <th>Start date</th>
          <th>End date</th>
          <th>Last modified</th>
        </tr>
      </thead>
      <#list submissions as submission>
        <tr>
          <td><a href="<@routes.mitcircs.editsubmission submission />">${submission.key}</a></td>
          <td>${submission.issueType.description}</td>
          <td><@fmt.date date=submission.startDate includeTime=false /></td>
          <td><@fmt.date date=submission.endDate includeTime=false /></td>
          <td><@fmt.date date=submission.lastModified /></td>
        </tr>
      </#list>
    </table>
  </#if>

</#escape>