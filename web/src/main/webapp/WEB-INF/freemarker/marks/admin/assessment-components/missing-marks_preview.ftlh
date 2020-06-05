<div class="deptheader">
  <h1>Missing marks adjustment</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_missingMarks assessmentComponent upstreamAssessmentGroup /></#assign>

<#if changes?size == 0>
  <p>There were no changes to marks or grades.</p>

  <p><a href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Back to assessment components</a></p>
<#else>
  <p>The following marks and grades will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped">
        <thead>
          <tr>
            <th>University ID</th>
            <th>Mark</th>
            <th>Grade</th>
            <th>Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as change>
            <#assign student = change._1() />
            <#assign existingMark = (change._2())!'-' />
            <#assign existingGrade = (change._3())!'-' />

            <tr>
              <td class="col-md-2">${student.universityId}</td>
              <td class="col-md-2">
                ${existingMark}
                <#if existingMark != '-'>
                  <i class="fal fa-arrow-right"></i>
                  -
                </#if>
              </td>
              <td class="col-md-2">
                ${existingGrade}
                <#if existingGrade! != 'FM'>
                  <i class="fal fa-arrow-right"></i>
                  FM
                </#if>
              </td>
              <td class="col-md-4">Assessment did not take place because of force majeure</td>
            </tr>
          </#list>
        </tbody>
      </table>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" value="Confirm" />
        <a class="btn btn-default dirty-check-ignore" href="${returnTo}">Cancel</a>
      </div>
    </@f.form>
  </div>
</#if>
