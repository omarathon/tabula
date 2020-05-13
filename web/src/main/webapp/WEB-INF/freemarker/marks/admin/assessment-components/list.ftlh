<#function route_function dept>
  <#local result><@routes.marks.assessmentcomponents dept academicYear /></#local>
  <#return result />
</#function>

<@fmt.id7_deptheader "Assessment components" route_function "for" />

<table class="table table-striped">
  <thead>
    <tr>
      <th rowspan="2">Module code</th>
      <th rowspan="2">Title</th>
      <th rowspan="2">Sequence</th>
      <th rowspan="2">Occurrence</th>
      <th rowspan="2">Type</th>
      <th rowspan="2">Weighting</th>
      <th rowspan="2">Initial deadline</th>
      <th colspan="2">Students</th>
      <th rowspan="2"></th>
    </tr>
    <tr>
      <th>Current</th>
      <th>With marks</th>
    </tr>
  </thead>
  <tbody>
    <#list results as result>
      <tr>
        <td>${result.assessmentComponent.moduleCode}</td>
        <td>${result.assessmentComponent.name}</td>
        <td>${result.assessmentComponent.sequence}</td>
        <td>${result.upstreamAssessmentGroupInfo.upstreamAssessmentGroup.occurrence}</td>
        <td>${result.assessmentComponent.assessmentType.name}</td>
        <td>${result.assessmentComponent.weighting}%</td>
        <td>
          <#if result.upstreamAssessmentGroupInfo.upstreamAssessmentGroup.deadline??>
            <@fmt.date date=result.upstreamAssessmentGroupInfo.upstreamAssessmentGroup.deadline includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
          <#else>
            <span class="very-subtle">Unknown</span>
          </#if>
        </td>
        <td>${result.upstreamAssessmentGroupInfo.currentMembers?size}</td>
        <td>${result.currentMembersWithMarksUploaded?size}</td>
        <td>
          <a class="btn btn-default btn-sm" href="<@routes.marks.assessmentcomponent_marks result.assessmentComponent result.upstreamAssessmentGroupInfo.upstreamAssessmentGroup />">Marks</a>
        </td>
      </tr>
    </#list>
  </tbody>
</table>
