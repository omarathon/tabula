<#import "*/mitcircs_components.ftlh" as components />
<#import "../profiles/profiles_macros.ftl" as profiles />

<@profiles.profile_header student isSelf />

<h1>Personal Circumstances</h1>

<#assign isPGR = ((student.mostSignificantCourse.courseType.code)!"") == 'PG(R)' />
<#if hasPermission>
  <#if !isPGR>
    <div class="pull-right">
      <a class="btn btn-primary" href="<@routes.mitcircs.newSubmission student />">Declare mitigating circumstances<#if !isSelf> on this student's behalf</#if></a>
    </div>
  </#if>

  <h2>Mitigating circumstances submissions</h2>
  <#if submissions?has_content>
    <table class="table table-condensed">
      <thead>
        <tr>
          <th class="col-sm-1">Reference</th>
          <th class="col-sm-3">Issue types</th>
          <th class="col-sm-2">Start date</th>
          <th class="col-sm-2">End date</th>
          <th class="col-sm-2">Last updated</th>
          <th class="col-sm-2">Submitted</th>
        </tr>
      </thead>
      <tbody>
        <#list submissions as submission>
          <tr>
            <td><a href="<@routes.mitcircs.viewSubmission submission />">MIT-${submission.key}</a></td>
            <td><@components.enumListWithOther submission.issueTypes submission.issueTypeDetails!"" /></td>
            <td><#if submission.startDate??><@fmt.date date=submission.startDate includeTime=false shortMonth=true excludeCurrentYear=true /><#else><span class="very-subtle">(not set)</span></#if></td>
            <td>
              <#if submission.endDate??>
                <@fmt.date date=submission.endDate includeTime=false shortMonth=true excludeCurrentYear=true />
              <#else>
                <span class="very-subtle">(not set)</span>
              </#if>
            </td>
            <td>
              <@fmt.date date=submission.lastModified shortMonth=true excludeCurrentYear=true />
              <#if submission.unreadByStudent>
                <span tabindex="0" class="tabula-tooltip" data-title="There are unread change(s)"><i class="far fa-envelope text-info"></i></span>
              </#if>
            </td>
            <td>
              <#if submission.withdrawn>
                <span class="very-subtle">(withdrawn)</span>
              <#elseif submission.approvedOn??>
                <@fmt.date date=submission.approvedOn shortMonth=true excludeCurrentYear=true />
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

<#if submissionsWithAcuteOutcomes?has_content>
  <h2>Acute mitigating circumstances</h2>

  <ul class="list-unstyled mitcircs__acute-outcomes">
    <#list submissionsWithAcuteOutcomes as info>
      <#assign submission = info.submission />
      <li>
        <h5 class="mitcircs__acute-outcomes__header">
          MIT-${submission.key}&nbsp;
          <span class="mitcircs__acute-outcomes__header__aside">
            <@fmt.date date=submission.startDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
            &mdash;
            <#if submission.endDate??>
              <@fmt.date date=submission.endDate includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
            <#else>
              <span class="very-subtle">(ongoing)</span>
            </#if>
          </span>
        </h5>

        <#if submission.acuteOutcome??>
          <div class="row form-horizontal mitcircs__acute-outcomes__section ">
            <div class="control-label">Outcomes</div>
            <div class="content form-control-static">
              ${submission.acuteOutcome.description} —  Outcomes recorded
              <#if submission.outcomesLastRecordedBy??>
                by ${submission.outcomesLastRecordedBy.fullName!submission.outcomesLastRecordedBy.userId}
              </#if>
              <#if submission.outcomesLastRecordedOn??>
                at <@fmt.date date=submission.outcomesLastRecordedOn />
              </#if>
            </div>
          </div>
        </#if>

        <#if info.affectedAssessments?has_content>
          <div class="row form-horizontal mitcircs__acute-outcomes__section ">
            <div class="control-label">Affected assessments</div>
            <div class="content form-control-static">
              <table class="table table-default mitcircs__acute-outcomes__affectedAssessments">
                <thead>
                  <tr>
                    <th class="col-sm-2">Module</th>
                    <th class="col-sm-4">Name</th>
                    <th class="col-sm-2">Deadline</th>
                    <th class="col-sm-2">Extension to be awarded</th>
                    <th class="col-sm-4">Tabula assignments</th>
                  </tr>
                </thead>
                <tbody>
                  <#list info.affectedAssessments?keys as assessment>
                    <tr>
                      <td><@components.assessmentModule assessment /></td>
                      <td>${assessment.name}</td>
                      <td><#if assessment.deadline??><@fmt.date date=assessment.deadline includeTime=false shortMonth=true excludeCurrentYear=true /><#else><span class="very-subtle">Unknown</span></#if></td>
                      <td><#if assessment.extensionDeadline??><@fmt.date date=assessment.extensionDeadline shortMonth=true excludeCurrentYear=true /><#else><span class="very-subtle">None specified</span></#if></td>
                      <td>
                        <#if mapGet(info.affectedAssessments, assessment)??>
                          <ul class="list-unstyled">
                            <#list mapGet(info.affectedAssessments, assessment) as assignmentInfo>
                              <#assign assignment = assignmentInfo.assignment />
                              <#assign url><#compress>
                                <#if submission.acuteOutcome.entryName == "WaiveLatePenalties">
                                  <@routes.cm2.feedbackAdjustment assignment />
                                <#elseif submission.acuteOutcome.entryName == "Extension">
                                  <@routes.cm2.assignmentextensions assignment />
                                <#else>
                                  <@routes.cm2.assignmentSubmissionSummary assignment />
                                </#if>
                              </#compress></#assign>
                              <li>
                                <a href="${url}">${assignment.name}</a>
                                <#if submission.acuteOutcome.entryName == "Extension">
                                  <#if assignmentInfo.hasExtension>
                                    <i class="fa fa-check-circle-o" data-toggle="tooltip" title="Extension granted"></i>
                                  <#else>
                                    <i class="fa fa-warning" data-toggle="tooltip" title="No extension granted"></i>
                                  </#if>
                                </#if>
                              </li>
                            </#list>
                          </ul>
                        </#if>
                      </td>
                    </tr>
                  </#list>
                </tbody>
              </table>
            </div>
          </div>
        </#if>
      </li>
    </#list>
  </ul>
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

            <#noautoesc>${student.formattedReasonableAdjustmentsNotes!''}</#noautoesc>
        </aside>
      </#if>
    </div>
  </section>
</#if>
