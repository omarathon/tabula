<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />

<div class="deptheader">
  <h1>Scaling</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_scaling assessmentComponent upstreamAssessmentGroup /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
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

      <table class="table table-striped table-sortable">
        <thead>
          <tr>
            <th class="col-sm-1 sortable">University ID</th>
            <th class="col-sm-2 sortable">First name</th>
            <th class="col-sm-2 sortable">Last name</th>
            <th class="col-sm-2 sortable">Mark</th>
            <th class="col-sm-2 sortable">Grade</th>
            <th class="col-sm-4">Comment</th>
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
            <#assign member = membersByUniversityId[student.universityId] />

            <tr<#if scaledGrade == 'F' && originalGrade != 'F'> class="danger"</#if>>
              <td data-sortby="u${student.universityId}">
                <@pl.profile_link member.universityId />
                ${student.universityId}
              </td>
              <td>${member.firstName}</td>
              <td>${member.lastName}</td>
              <td data-sortby="${change._2()._2()!-1}">
                ${originalMark}
                <#if originalMark != scaledMark>
                  <i class="fal fa-arrow-right"></i>
                  ${scaledMark}
                </#if>
              </td>
              <td data-sortby="g${scaledGrade}">
                ${originalGrade}
                <#if originalGrade != scaledGrade>
                  <i class="fal fa-arrow-right"></i>
                  ${scaledGrade}
                </#if>
              </td>
              <td>${comment}</td>
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
