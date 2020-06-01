<div class="deptheader">
  <h1>Scaling</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_scaling assessmentComponent upstreamAssessmentGroup /></#assign>

<#if changes?size == 0>
  <p>There were no changes to marks or grades.</p>

  <p><a href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Back to assessment components</a></p>
<#else>
  <p>The following marks and grades will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />
      <@f.hidden path="calculate" />
      <@f.hidden path="passMark" />
      <@f.hidden path="scaledPassMark" />
      <@f.hidden path="scaledUpperClassMark" />

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
            <#assign originalMark = change._2()._1()!'-' />
            <#assign scaledMark = change._2()._2()!'-' />
            <#assign originalGrade = change._3()._1()!'-' />
            <#assign scaledGrade = change._3()._2()!'-' />
            <#assign comment = change._4() />

            <tr>
              <td class="col-md-2">${student.universityId}</td>
              <td class="col-md-2">
                ${originalMark}
                <#if originalMark != scaledMark>
                  <i class="fal fa-arrow-right"></i>
                  ${scaledMark}
                </#if>
              </td>
              <td class="col-md-2">
                ${originalGrade}
                <#if originalGrade != scaledGrade>
                  <i class="fal fa-arrow-right"></i>
                  ${scaledGrade}
                </#if>
              </td>
              <td class="col-md-4">${comment}</td>
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
