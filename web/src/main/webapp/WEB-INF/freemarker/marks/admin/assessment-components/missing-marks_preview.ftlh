<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />

<div class="deptheader">
  <h1>Missing marks adjustment</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_missingMarks assessmentComponent upstreamAssessmentGroup /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if changes?size == 0>
  <p>There were no changes to marks or grades.</p>

  <p><a href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Back to assessment components</a></p>
<#else>
  <p>The following marks and grades will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped table-sortable">
        <thead>
          <tr>
            <th class="col-sm-1 sortable">University ID</th>
            <th class="col-sm-2 sortable">First name</th>
            <th class="col-sm-2 sortable">Last name</th>
            <th class="col-sm-2">Mark</th>
            <th class="col-sm-2">Grade</th>
            <th class="col-sm-4">Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as change>
            <#assign student = change._1() />
            <#assign existingMark = (change._2())!'-' />
            <#assign existingGrade = (change._3())!'-' />
            <#assign member = membersByUniversityId[student.universityId] />

            <tr>
              <td data-sortby="u${student.universityId}">
                <@pl.profile_link member.universityId />
                ${student.universityId}
              </td>
              <td>${member.firstName}</td>
              <td>${member.lastName}</td>
              <td>
                ${existingMark}
                <#if existingMark != '-'>
                  <i class="fal fa-arrow-right"></i>
                  -
                </#if>
              </td>
              <td>
                ${existingGrade}
                <#if existingGrade! != 'FM'>
                  <i class="fal fa-arrow-right"></i>
                  FM
                </#if>
              </td>
              <td>Assessment did not take place because of force majeure</td>
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
