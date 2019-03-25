<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/cm2_macros.ftl" as cm2 />
<#assign currentPage = command.page />
<#assign totalPages = (results.total / command.extensionsPerPage)?ceiling />

<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="row extension-metadata">
  <div class="col-md-7">
    <p>Found <@fmt.p results.total "extension" />.</p>
  </div>
  <div class="col-md-5">
    <p class="alert alert-info">
      Students will automatically be notified by email when you approve, modify or revoke an extension.
    </p>
  </div>
  <div class="col-md-12">
    <@cm2.pagination currentPage totalPages />
  </div>
</div>

<table class="students table table-striped sticky-table-headers expanding-table">
  <thead>
    <tr>
      <th>First name</th>
      <th>Last name</th>
      <th>University ID</th>
      <th>Module</th>
      <th>Assignment</th>
      <th>Status</th>
      <th>Extension length</th>
      <th>Submission due</th>
    </tr>
  </thead>
  <tbody>
  <#list results.extensions as graph>
    <tr data-toggle="collapse" data-target="#extension${graph.extension.id}" class="clickable collapsed expandable-row">
      <#-- TAB-2063 - The extension manager will need to know who is doing the asking, so we should always show names -->
      <td class="student-col toggle-cell toggle-icon">${graph.user.firstName}</td>
      <td class="student-col toggle-cell">${graph.user.lastName}</td>
      <td class="id toggle-cell">
        <#assign identifier = graph.user.warwickId!graph.user.userId />
        ${identifier} <@pl.profile_link identifier />
      </td>
      <td><@fmt.module_name graph.extension.assignment.module false /></td>
      <td><a href="<@routes.cm2.assignmentextensions graph.extension.assignment />">${graph.extension.assignment.name}</a></td>
      <td>${graph.extension.state.description}</td>
      <td data-datesort="<#if (graph.duration > 0)>${graph.duration}<#elseif (graph.requestedExtraDuration > 0) >${graph.requestedExtraDuration}<#else>0</#if>">
        <#if graph.hasApprovedExtension || graph.isAwaitingReview() || graph.extension.moreInfoRequired>
          <#if graph.duration != 0>
            <@fmt.p graph.duration "day"/>
          <#else>
            <@fmt.p graph.requestedExtraDuration "day"/>
          </#if>
        </#if>
      </td>
      <td <#if graph.deadline?has_content>data-datesort="${graph.deadline.millis?c!''}"</#if>
          class="deadline-col <#if graph.hasApprovedExtension>approved<#else>very-subtle</#if>">
        <#if graph.deadline?has_content><@fmt.date date=graph.deadline /></#if>
      </td>
    </tr>
    <#assign detailUrl><@routes.cm2.extensionDetail graph.extension /></#assign>
    <tr id="extension${graph.extension.id}" data-detailurl="${detailUrl}" class="collapse detail-row">
      <td colspan="7" class="detailrow-container">
        <i class="fa fa-spinner fa-spin"></i> Loading
      </td>
    </tr>
  </#list>
  </tbody>
</table>

<script type="text/javascript">
  (function ($) {
    // add a custom parser for the date column
    $.tablesorter.addParser({
      id: 'customdate',
      is: function (s, table, cell, $cell) {
        return false; /*return false so this parser is not auto detected*/
      },
      format: function (s, table, cell, cellIndex) {
        var $cell = $(cell);
        return $cell.attr('data-datesort') || s;
      },
      parsed: false,
      type: 'numeric'
    });


    $('.expanding-table').expandingTable({
      contentUrlFunction: function ($row) {
        return $row.data('detailurl');
      },
      useIframe: true,
      tableSorterOptions: {
        sortList: [[1, 0], [0, 0]],
        headers: {
          6: {sorter: 'customdate'},
          7: {sorter: 'customdate'}
        }
      },
      preventContentIdInUrl: true
    });
  })(jQuery);
</script>