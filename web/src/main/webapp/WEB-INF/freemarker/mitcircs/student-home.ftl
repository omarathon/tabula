<#escape x as x?html>
  <#if !isSelf>
    <details class="indent">
      <summary>${student.fullName}</summary>
      <#if student.userId??>
        ${student.userId}<br />
      </#if>
      <#if student.email??>
        <a href="mailto:${student.email}">${student.email}</a><br />
      </#if>
      <#if student.phoneNumber??>
        ${phoneNumberFormatter(student.phoneNumber)}<br />
      </#if>
      <#if student.mobileNumber??>
        ${phoneNumberFormatter(student.mobileNumber)}<br />
      </#if>
    </details>
  </#if>

  <h1>Personal Circumstances</h1>

  <#if hasPermission>
    <div class="pull-right">
      <a class="btn btn-primary" href="<@routes.mitcircs.newSubmission student />">Declare mitigating circumstances<#if !isSelf> on this student's behalf</#if></a>
    </div>
    <h2>Mitigating circumstances submissions</h2>
    <#if submissions?has_content>
      <table class="table table-condensed">
        <thead>
          <tr>
            <th>Reference</th>
            <th>Issue types</th>
            <th>Start date</th>
            <th>End date</th>
            <th>Last modified</th>
            <th>Submitted</th>
          </tr>
        </thead>
        <tbody>
          <#list submissions as submission>
            <tr>
              <td><a href="<@routes.mitcircs.viewSubmission submission />">MIT-${submission.key}</a></td>
              <td><#if submission.issueTypes?has_content><#list submission.issueTypes as type>${type.description}<#if type_has_next>, </#if></#list></#if></td>
              <td><@fmt.date date=submission.startDate includeTime=false /></td>
              <td>
                <#if submission.endDate??>
                  <@fmt.date date=submission.endDate includeTime=false />
                <#else>
                  <span class="very-subtle">(not set)</span>
                </#if>
              </td>
              <td>
                <@fmt.date date=submission.lastModified /> by <#if submission.lastModifiedBy == user.apparentUser>you<#else>${submission.lastModifiedBy.fullName}</#if>
              </td>
              <td>
                <#if submission.withdrawn>
                  <span class="very-subtle">(withdrawn)</span>
                <#elseif submission.approvedOn??>
                  <@fmt.date date=submission.approvedOn />
                <#else>
                  <span class="very-subtle">(draft)</span>
                </#if>
              </td>
            </tr>
          </#list>
        <tbody>
      </table>
    <#else>
      <p><#if isSelf>You have<#else>This student has</#if> not declared any mitigating circumstances.</p>
    </#if>
  <#else>
    <div class="alert alert-info">
      You do not have permission to see the personal circumstances of this student.
    </div>
  </#if>

  <#if student.reasonableAdjustments?has_content || student.reasonableAdjustmentsNotes?has_content>
    <section class="mitcircs__reasonable-adjustments media">
      <div class="media-left">
        <i class="fal fa-info-circle"></i>
      </div>
      <div class="media-body">
        <header class="mitcircs__reasonable-adjustments__header media-heading">
          <h2>Reasonable adjustments</h2>
          <span class="mitcircs__reasonable-adjustments__header__aside">(provided by Wellbeing Support Services)</span>
        </header>

        <#if student.reasonableAdjustments?has_content>
          <ul class="fa-ul">
              <#list student.reasonableAdjustments?sort_by('id') as reasonableAdjustment>
                <li><span class="fa-li"><i class="fal fa-check"></i></span>${reasonableAdjustment.description}</li>
              </#list>
          </ul>
        </#if>

        <#if student.reasonableAdjustmentsNotes?has_content>
          <aside>
            <h3>Notes from Wellbeing Support</h3>

              <#noescape>${student.formattedReasonableAdjustmentsNotes!''}</#noescape>
          </aside>
        </#if>
      </div>
    </section>
  </#if>

</#escape>