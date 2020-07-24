<#function route_function dept>
  <#local result><@routes.marks.assessmentcomponents dept academicYear /></#local>
  <#return result />
</#function>

<@fmt.id7_deptheader "Assessment components" route_function "for" />

<table class="table table-striped table-sortable">
  <thead>
    <tr>
      <th class="sortable" rowspan="2">Module code</th>
      <th class="sortable" rowspan="2">Title</th>
      <th class="sortable" rowspan="2">Sequence</th>
      <th class="sortable" rowspan="2">Occurrence</th>
      <th class="sortable" rowspan="2">Type</th>
      <th class="sortable" rowspan="2">Weighting</th>
      <th class="sortable" rowspan="2">Initial deadline</th>
      <th colspan="2">Students</th>
      <th rowspan="2"></th>
    </tr>
    <tr>
      <th class="sortable">All</th>
      <th class="sortable">With marks</th>
    </tr>
  </thead>
  <tbody>
    <#list results as result>
      <tr>
        <td>${result.assessmentComponent.moduleCode}</td>
        <td>${result.assessmentComponent.name}</td>
        <td>${result.assessmentComponent.sequence}</td>
        <td>${result.upstreamAssessmentGroup.occurrence}</td>
        <td>${result.assessmentComponent.assessmentType.name}</td>
        <td>${(result.assessmentComponent.scaledWeighting!0)?string["0.#"]}%</td>
        <td data-sortby="${(result.upstreamAssessmentGroup.deadline.toString())!'1970-01-01'}">
          <#if result.upstreamAssessmentGroup.deadline??>
            <@fmt.date date=result.upstreamAssessmentGroup.deadline includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
          <#else>
            <span class="very-subtle">Unknown</span>
          </#if>
        </td>
        <td>${result.students?size}</td>
        <td data-sortby="${result.studentsWithMarks?size}">
          ${result.studentsWithMarks?size}
          <#if result.needsWritingToSits>
            <span tabindex="0" class="tabula-tooltip" data-title="Waiting to be written to SITS">
              <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
              <span class="sr-only">Waiting to be written to SITS</span>
            </span>
          <#elseif result.outOfSync>
            <span tabindex="0" class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
              <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
              <span class="sr-only">Mark is out of sync with current mark in SITS</span>
            </span>
          <#elseif result.allAgreed>
            <span tabindex="0" class="tabula-tooltip" data-title="Agreed marks published to students">
              <i class="fa-fw fad fa-check-circle" aria-hidden="true"></i>
              <span class="sr-only">Agreed marks published to students</span>
            </span>
          </#if>
        </td>
        <td>
          <a class="btn btn-default btn-sm" href="<@routes.marks.assessmentcomponent_marks result.assessmentComponent result.upstreamAssessmentGroup />">Marks</a>
        </td>
      </tr>
    </#list>
  </tbody>
</table>
