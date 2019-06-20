<#escape x as x?html>

    <#import "*/_profile_link.ftl" as pl />
  <div id="profile-modal" class="modal fade profile-subset"></div>

    <#macro attendanceState state>
        <#switch state.dbValue>
            <#case "attended">
              <i class="fa fa-fw fa-check attended"></i>
                <#break>
            <#case "authorised">
              <i class="fa fa-fw fa-times-circle-o authorised"></i>
                <#break>
            <#case "unauthorised">
              <i class="fa fa-fw fa-times unauthorised"></i>
                <#break>
            <#default>
              <i class="fa fa-fw fa-minus"></i>
                <#break>
        </#switch>
    </#macro>

  <div class="deptheader">
    <h1>Check and update attendance</h1>
    <h5 class="with-related"><span class="muted">for</span> ${command.templatePoint.name}</h5>
  </div>

    <#if command.proposedChanges?has_content>
        <@f.form method="post" modelAttribute="command" cssClass="double-submit-protection">
            <@form.errors path="" />
            <#if command.otherToAttended?has_content>
              <p>
                  <#if command.otherToAttended?size == 1>
                    1 more student meets
                  <#else>
                      ${command.otherToAttended?size?c} more students meet
                  </#if>
                the conditions to be automatically marked as 'attended' for this monitoring point.
              </p>
            </#if>

            <#if command.attendedToOther?has_content>
              <p>
                  <#if command.attendedToOther?size == 1>
                    1 student no longer meets
                  <#else>
                      ${command.attendedToOther?size?c} students no longer meet
                  </#if>
                the conditions to be automatically marked as 'attended' for this monitoring point.
              </p>
            </#if>

          <table class="table table-striped">
            <thead>
            <tr>
              <th>University ID</th>
              <th>First name</th>
              <th>Last name</th>
              <th>Current attendance</th>
              <th>Proposed attendance</th>
              <th></th>
            </tr>
            </thead>
            <tbody>
            <#list command.proposedChanges as change>
              <tr>
                <td>
                    ${change.student.universityId}
                    <@pl.profile_link change.student.universityId />
                </td>
                <td>${change.student.firstName}</td>
                <td>${change.student.lastName}</td>
                <td>
                    <@attendanceState change.currentState />
                </td>
                <td>
                    <@attendanceState change.proposedState />
                </td>
                <td>
                  <#if change.alreadyReported>
                    <a class="use-tooltip" title="This change cannot be applied automatically. Attendance data for ${change.student.firstName} has already been reported to SITS.">
                      <i class="fa text-danger fa-exclamation-triangle"></i>
                    </a>
                  </#if>
                </td>
              </tr>
            </#list>
            </tbody>
          </table>

            <#if command.proposedChangesToAlreadyReportedPoints?has_content>
              <div class="alert alert-info">
                Attendance for
                <#if command.proposedChangesToAlreadyReportedPoints?size == 1>
                  one student
                <#else>
                    ${command.proposedChangesToAlreadyReportedPoints?size?c} students
                </#if>
                has already been reported to SITS for this monitoring period.

                <#if command.proposedChangesToAlreadyReportedPoints?size == 1>
                  This change
                <#else>
                  These changes
                </#if>
                cannot be applied automatically.
              </div>
            </#if>

          <p>
            <#if command.proposedChangesToNonReportedPoints?has_content>
              <button type="submit" class="btn btn-primary">Apply changes</button>
            </#if>
            <a href="${returnTo}" class="btn btn-default">Leave unchanged</a>
          </p>
        </@f.form>
    <#else>
      <p>
        The conditions for this monitoring point have been re-checked. There are no proposed changes to attendance.
      </p>

      <p>
        <a href="${returnTo}" class="btn btn-default">Continue</a>
      </p>
    </#if>

</#escape>

