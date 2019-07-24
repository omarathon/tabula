<#import "*/mitcircs_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#escape x as x?html>
  <#if submissions?has_content>
    <table class="table table-condensed table-sortable">
      <thead>
        <tr>
          <#if can.do("MitigatingCircumstancesPanel.Modify", department)><th><input type="checkbox" class="check-all" title="Select all/none"></th></#if>
          <th class="sortable">Reference</th>
          <th class="sortable">Student</th>
          <th class="sortable">Affected dates</th>
          <th class="col-sm-2 sortable">Progress</th>
          <th class="sortable">Panel</th>
          <th class="col-sm-2 sortable">Last updated</th>
        </tr>
      </thead>
      <tbody>
      <#list submissions as info>
        <#assign submission = info.submission />
        <tr>
          <#if can.do("MitigatingCircumstancesPanel.Modify", department)><td><input type="checkbox" name="submissions" value="${submission.key}"></td></#if>
          <td>
            <#if !submission.draft>
              <a href="<@routes.mitcircs.reviewSubmission submission />">MIT-${submission.key}</a>
            <#else>
              MIT-${submission.key}
            </#if>
          </td>
          <td data-sortby="${submission.student.lastName}, ${submission.student.firstName}, ${submission.student.universityId}">
            <@pl.profile_link submission.student.universityId />
            ${submission.student.universityId}
            ${submission.student.firstName}
            ${submission.student.lastName}
          </td>
          <td data-sortby="${(submission.startDate.toString())!'1970-01-01'}">
            <#if submission.startDate??>
              <@fmt.date date=submission.startDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
              &mdash;
              <#if submission.endDate??>
                <@fmt.date date=submission.endDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
              <#else>
                <span class="very-subtle">(ongoing)</span>
              </#if>
            <#else>
              <span class="very-subtle">TBC</span>
            </#if>
          </td>
          <td data-sortby="${components.stage_sortby(info.stages?values)}"><@components.stage_progress_bar info.stages?values /></td>
          <td<#if !submission.panel??> data-sortby="<#if submission.acute>zzz-1<#else>zzz-2</#if>"</#if>>
            <#if submission.panel??>
              <a href="<@routes.mitcircs.viewPanel submission.panel />">${submission.panel.name}</a>
            <#elseif submission.acute>
              <span class="very-subtle">N/A</span>
            <#else>
              <span class="very-subtle">TBC</span>
            </#if>
          </td>
          <td data-sortby="${submission.lastModified.millis}">
            <@fmt.date date=submission.lastModified shortMonth=true excludeCurrentYear=true />
            <#if submission.unreadByOfficer>
              <span tabindex="0" role="button" class="tabula-tooltip" data-title="There are unread change(s)"><i class="far fa-envelope text-info"></i></span>
            </#if>
          </td>
        </tr>
      </#list>
      <tbody>
    </table>
  <#else>
    <p>No mitigating circumstances submissions found.</p>
  </#if>

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      $('a.ajax-modal').ajaxModalLink();

      $('.table-sortable').sortableTable({
        // Default is to sort by last updated date, descending
        sortList: [[6, 1]],

        // If there's a data-sortby, use that as the sort value
        textExtraction: function (node) {
          var $el = $(node);
          if ($el.data('sortby')) {
            return $el.data('sortby');
          } else {
            return $el.text().trim();
          }
        }
      });

      // We probably just grew a scrollbar, so let's trigger a window resize
      $(window).trigger('resize.ScrollToFixed');
    })(jQuery);
  </script>
</#escape>