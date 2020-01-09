<h1>Mandatory departmental permissions</h1>

<p>All departments <strong>MUST</strong> have:</p>

<ul>
  <li>A User Access Manager who acts on the Head of Department's authority to manage permissions in Tabula</li>
  <li>(If a department has UG or PGT students) a Mitigating Circumstances Officer who is responsible for responding to and handling claims for mitigation from students</li>
</ul>

<p>This takes into account a student's <em>enrolment</em> department (also known as their home department).</p>

<#macro infoRow info prefix="" parentInfo={}>
  <tr>
    <td>${prefix} ${info.department.code?upper_case}</td>
    <td>
      ${info.department.name}
      <#if info.department.hasParent>
        <div class="help-block">Filter: ${info.department.filterRule.name}</div>
      </#if>
    </td>
    <#local needsMCO = false />
    <#list courseTypes as courseType>
      <#if courseType.code != 'PG(R)'>
        <#local needsMCO = needsMCO || mapGet(info.activeStudents, courseType) gt 0 />
      </#if>
      <#if mapGet(info.activeStudents, courseType) gt 0>
        <td>${mapGet(info.activeStudents, courseType)}</td>
      <#else>
        <td>-</td>
      </#if>
    </#list>
    <#if info.hasActiveUAM>
      <td class="success">Yes</td>
    <#elseif (parentInfo.hasActiveUAM)!false>
      <td class="info">Yes (from parent)</td>
    <#else>
      <td class="danger">No</td>
    </#if>
    <#if needsMCO>
      <#if info.department.enableMitCircs>
        <td class="success">Yes</td>
      <#elseif (parentInfo.department.enableMitCircs)!false>
        <td class="info">Yes (to parent)</td>
      <#else>
        <td class="danger">
          <a href="<@url page="/sysadmin/mitcircs/check" />#${info.department.code}">No</a>
        </td>
      </#if>
      <#if info.department.enableMitCircs && info.hasActiveMCO>
        <td class="success">Yes</td>
      <#elseif (parentInfo.hasActiveMCO)!false>
        <td class="info">Yes (to parent)</td>
      <#else>
        <td class="danger">
          <a href="<@url page="/sysadmin/mitcircs/check" />#${info.department.code}">No</a>
        </td>
      </#if>
    <#else>
      <td>-</td>
      <td>-</td>
    </#if>
  </tr>
  <#list info.children as child>
    <@infoRow child "${prefix} ┗" info />
  </#list>
</#macro>

<table class="table table-striped table--departments">
  <thead>
    <tr>
      <th rowspan="2">Code</th>
      <th rowspan="2">Name</th>
      <th colspan="${courseTypes?size}">Active students</th>
      <th rowspan="2">UAM?</th>
      <th rowspan="2">Mit circs enabled?</th>
      <th rowspan="2">MCO?</th>
    </tr>
    <tr>
      <#list courseTypes as courseType>
        <th>${courseType.description}</th>
      </#list>
    </tr>
  </thead>
  <tbody>
    <#list departmentInfo as info>
      <@infoRow info />
    </#list>
  </tbody>
</table>

<script type="text/javascript" nonce="${nonce()}">
  jQuery(function ($) {
    var hidden = false;
    var $button =
      $('<button />')
        .attr('type', 'button')
        .addClass('btn btn-default')
        .text('Hide departments without any issues');

    var $table = $('.table--departments');

    $button.on('click', function () {
      if (hidden) {
        $table.find('tbody tr').filter(function (i, row) {
          return $(row).find('td.danger').length === 0;
        }).show();
        hidden = false;
        $button.text('Hide departments without any issues');
      } else {
        $table.find('tbody tr').filter(function (i, row) {
          return $(row).find('td.danger').length === 0;
        }).hide();
        hidden = true;
        $button.text('Show departments without any issues');
      }
    });

    $table.before($button);
  });
</script>
