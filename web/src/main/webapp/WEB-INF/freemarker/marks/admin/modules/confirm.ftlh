<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />

<div class="deptheader">
  <h1>Confirm module marks</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#assign submitUrl><@routes.marks.module_marks_confirm sitsModuleCode academicYear occurrence /></#assign>
<#assign cancelUrl><@routes.marks.adminhome module.adminDepartment academicYear /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if alreadyConfirmed?has_content>
  <div class="alert alert-success">
      <@fmt.p number=alreadyConfirmed?size singular="student" plural="students" /> marks are already confirmed
  </div>
</#if>
<#if studentsToConfirm?has_content>
  <p>
    The following module results and all component results will be confirmed as actual marks.
    Other departments will be made aware that these marks have been approved by a Pre-examination Module Board.
    Any subsequent changes to confirmed actual marks <strong>must</strong> be communicated to any other departments with students taking this module.
  </p>
<#else>
  <div class="alert alert-info">There are no students with unconfirmed actual marks for this module.</div>
</#if>

<div class="fix-area">
  <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <@f.hidden name="confirm" value="true" />
    <#if studentModuleRecords?has_content>
      <table class="table table-striped table-sortable">
        <thead>
        <tr>
          <th class="col-sm-1 sortable">SPR Code</th>
          <th class="col-sm-1 sortable">First name</th>
          <th class="col-sm-1 sortable">Last name</th>
          <th class="col-sm-1 sortable">Mark</th>
          <th class="col-sm-1 sortable">Grade</th>
          <th class="col-sm-1 sortable">Result</th>
          <th class="col-sm-4">Comment</th>
        </tr>
        </thead>
        <tbody>
        <#list studentsToConfirm as record>
          <#assign student = record._1() />
          <#assign member = membersBySprCode[student.sprCode] />
          <tr>
            <td data-sortby="${student.sprCode}">
              <@pl.profile_link member.universityId />
              ${student.sprCode}
            </td>
            <td>${member.firstName}</td>
            <td>${member.lastName}</td>
            <td data-sortby="${student.mark!-1}">
              ${student.mark!'-'}
            </td>
            <td data-sortby="${student.grade!}">
              ${student.grade!'-'}
            </td>
            <td data-sortby="${(student.result.dbValue)!}">
              ${(student.result.dbValue)!'-'}
            </td>
            <td><@f.input path="comments[${student.sprCode}]" cssClass="form-control" /></td>
          </tr>
        </#list>
        <#list alreadyConfirmed as record>
          <#assign student = record._1() />
          <#assign member = membersBySprCode[student.sprCode] />
          <tr class="info">
            <td data-sortby="${student.sprCode}">
              <@pl.profile_link member.universityId />
              ${student.sprCode} - already confirmed
            </td>
            <td>${member.firstName}</td>
            <td>${member.lastName}</td>
            <td data-sortby="${student.mark!-1}">
              ${student.mark!'-'}
            </td>
            <td data-sortby="${student.grade!}">
              ${student.grade!'-'}
            </td>
            <td data-sortby="${(student.result.dbValue)!}">
              ${(student.result.dbValue)!'-'}
            </td>
            <td><@f.input path="comments[${student.sprCode}]" cssClass="form-control" readonly=true /></td>
          </tr>
        </#list>
        </tbody>
      </table>
    </#if>

    <div class="submit-buttons fix-footer">
      <#if studentsToConfirm?has_content><input type="submit" class="btn btn-primary" value="Confirm" /></#if>
      <a class="btn btn-default dirty-check-ignore" href="${cancelUrl?markup_string}">Cancel</a>
    </div>
  </@f.form>
</div>
