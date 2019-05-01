<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#escape x as x?html>
  <#if submissions?has_content>
    <table class="table table-condensed">
      <thead>
        <tr>
          <th>Reference</th>
          <th>University ID</th>
          <th>First name</th>
          <th>Last name</th>
          <th>Issue type</th>
          <th>Start date</th>
          <th>End date</th>
          <th>State</th>
          <th>Last modified</th>
        </tr>
      </thead>
      <tbody>
      <#list submissions as submission>
        <tr>
          <td><a href="">MIT-${submission.key}</a></td>
          <td>${submission.student.universityId} <@pl.profile_link submission.student.universityId /></td>
          <td>${submission.student.firstName}</td>
          <td>${submission.student.lastName}</td>
          <td><#if submission.issueTypes?has_content><#list submission.issueTypes as type>${type.description}<#if type_has_next>, </#if></#list></#if></td>
          <td><@fmt.date date=submission.startDate includeTime=false /></td>
          <td>
            <#if submission.endDate??>
              <@fmt.date date=submission.endDate includeTime=false />
            <#else>
              <span class="very-subtle">(not set)</span>
            </#if>
          </td>
          <td>${submission.state.entryName}</td>
          <td><@fmt.date date=submission.lastModified /></td>
        </tr>
      </#list>
      <tbody>
    </table>
  <#else>
    <p>There are no mitigating circumstances submissions for ${department.name}.</p>
  </#if>

  <script type="text/javascript">
    // We probably just grew a scrollbar, so let's trigger a window resize
    (function ($) {
      $(window).trigger('resize.ScrollToFixed');
    })(jQuery);
  </script>
</#escape>