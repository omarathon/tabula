<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#escape x as x?html>

  <div id="profile-modal" class="modal fade profile-subset"></div>

  <#function route_function dept>
    <#local result><@routes.mitcircs.adminhome dept /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Create a mitigating circumstances panel" route_function "for" />

  <@f.form method="POST" modelAttribute="createCommand" class="mitcircs-panel-form dirty-check double-submit-protection" enctype="multipart/form-data">

    <@bs3form.labelled_form_group path="name" labelText="Name">
      <@f.input path="name" cssClass="form-control" />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="date" labelText="Panel date">
      <div class="input-group">
        <@f.input path="date" autocomplete="off" cssClass="form-control date-picker" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
      </div>
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="members" labelText="Panel members">
      <@bs3form.flexipicker path="members" placeholder="User name" list=true multiple=true auto_multiple=false />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="submissions" labelText="Submissions">
      <#if createCommand.submissions?has_content>
        <table class="mitcircs-panel-form__submissions table table-condensed">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Student</th>
              <th>Affected dates</th>
              <th>Last updated</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
          <#list createCommand.submissions as submission>
            <tr>
              <td>
                <a href="<@routes.mitcircs.reviewSubmission submission />">MIT-${submission.key}</a>
                <@f.hidden path="submissions" value="${submission.key}" />
              </td>
              <td>
                ${submission.student.universityId} <@pl.profile_link submission.student.universityId />
                ${submission.student.firstName}
                ${submission.student.lastName}
              </td>
              <td>
                <@fmt.date date=submission.startDate includeTime=false relative=false />
                &mdash;
                <#if submission.endDate??>
                  <@fmt.date date=submission.endDate includeTime=false relative=false />
                <#else>
                  <span class="very-subtle">(ongoing)</span>
                </#if>
              </td>
              <td>
                <@fmt.date date=submission.lastModified />
                <#if submission.unreadByOfficer>
                  <span class="tabula-tooltip" data-title="There are unread change(s)"><i class="far fa-envelope text-info"></i></span>
                </#if>
              </td>
              <td>
                <button class="remove btn btn-sm">Remove</button>
              </td>
            </tr>
          </#list>
          </tbody>
        </table>
      <#else>
        <div class="form-control-static">No submissions have been added to this panel</div>
      </#if>
    </@bs3form.labelled_form_group>

    <div class="fix-footer">
      <button type="submit" class="btn btn-primary" name="submit">Create panel</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.adminhome department academicYear/>">Cancel</a>
    </div>

  </@f.form>
</#escape>