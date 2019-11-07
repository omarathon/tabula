<#import "/WEB-INF/freemarker/mitcircs/mitcircs_components.ftlh" as components />

<div class="exam-grid-preview">
  <div class="deptheader">
    <#if department.examGridOptions.nameToShow.toString != 'none'>
      <h1>${member.fullName!}</h1>
      <h4 class="with-related">${member.universityId}</h4>
    <#else>
      <h1>${member.universityId}</h1>
    </#if>
  </div>
  <div class="key clearfix">
    <table class="table table-condensed">
      <thead>
      <tr>
        <th colspan="2">Report</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <th>Department:</th>
        <td>${studentCourseDetails.department.name}</td>
      </tr>
      <tr>
        <th>Course:</th>
        <td>${studentCourseDetails.course.code?upper_case} ${studentCourseDetails.course.name}</td>
      </tr>
      <tr>
        <th>Route:</th>
        <td>${studentCourseDetails.currentRoute.code?upper_case} ${studentCourseDetails.currentRoute.name}</td>
      </tr>
      <tr>
        <th>Academic Year:</th>
        <td>${academicYear.startYear?c}</td>
      </tr>
      <tr>
        <th>Study Year:</th>
        <td> ${command.studentCourseYearDetails.yearOfStudy}</td>
      </tr>

      <tr>
        <th>Year weightings:</th>
        <td>
          <#list weightings as weighting>
            Year ${weighting.yearOfStudy} = ${weighting.weightingAsPercentage}%<#if weighting_has_next> | </#if>
          </#list>
        </td>
      </tr>
      <tr>
        <th>Normal CAT load:</th>
        <td>
          <#if normalLoadLookup.withoutDefault(studentCourseDetails.currentRoute)?has_content>
            ${normalLoadLookup.withoutDefault(studentCourseDetails.currentRoute)}
          <#else>
            <#assign defaultNormalLoad>${normalLoadLookup.apply(studentCourseDetails.currentRoute)}</#assign>
            ${defaultNormalLoad} <@fmt.help_popover id="normal-load" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
          </#if>
        </td>
      </tr>
      </tbody>
    </table>

    <table class="table table-condensed">
      <thead>
      <tr>
        <th colspan="2">Key</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td><span class="exam-grid-fail">#</span></td>
        <td>Failed module or component</td>
      </tr>
      <tr>
        <td><span class="exam-grid-actual-mark">#</span></td>
        <td>Agreed mark missing, using actual</td>
      </tr>
      <tr>
        <td><span class="exam-grid-resit"># (#)</span></td>
        <td>Resit mark (original mark)</td>
      </tr>
      <tr>
        <td><span class="exam-grid-actual-mark">X</span></td>
        <td>Agreed and actual mark missing</td>
      </tr>
      <tr>
        <td></td>
        <td>Blank indicates module not taken by student</td>
      </tr>
      </tbody>
    </table>
  </div>

  <div class="assessment_details">
    <table class="table table-condensed grid no-wide-tables">
      <thead>
      <tr class="assessment_componentdetails_header">
        <th>Module</th>
        <th>Module Credit</th>
        <th>Seq</th>
        <th>Assessment</th>

        <th>Type</th>
        <th>Component Mark</th>
        <th>Component Grade</th>
        <th class="header_col">Module Mark</th>
        <th class="header_col">Module Grade</th>
      </tr>
      </thead>
      <tbody>
      <#list assessmentComponents as info>
        <#assign mr = info.moduleRegistration />
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <div>${mr.module.code?upper_case}</div>
            <div>${mr.module.name}</div>
          </td>
          <td class="assessment_details_col">${mr.cats}</td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>${component.upstreamGroup.group.sequence}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>${component.upstreamGroup.name}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td class="assessment_details_col"><#if mr.selectionStatus??>${(mr.selectionStatus.description)!}<#else>-</#if></td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>
                    <#if component.member.firstDefinedMark??>
                      <#assign passMark =mapGet(passMarkMap, component.upstreamGroup.assessmentComponent.module) />
                      <#assign class><#compress>
                        <#if component.member.firstDefinedMark?number < passMark>exam-grid-fail </#if>
                        <#if !component.member.isAgreedMark()>exam-grid-actual-mark </#if>
                        <#if component.member.isResitMark()>exam-grid-resit </#if>
                      </#compress></#assign>
                      <span class="${class}"><#compress>
                            ${component.member.firstDefinedMark}
                            <#if component.member.isResitMark() && component.member.firstOriginalMark??>
                          (${component.member.firstOriginalMark})
                        </#if>
                          </#compress></span>
                    <#else>
                      <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No marks set for Assessment component">X</span>
                    </#if>
                  </td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>
                    <#if component.member.firstDefinedGrade??>
                      <span class="<#if !component.member.isAgreedGrade()>exam-grid-actual-mark</#if>"> ${component.member.firstDefinedGrade}</span>
                    <#else>
                      <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No grade set for Assessment component">X</span>
                    </#if>
                  </td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td class="assessment_details_col assessment_details_col1">
            <#assign passMark = mapGet(passMarkMap, mr.module) />
            <#if mr.agreedMark??>
              <#if mr.agreedMark?number lt passMark>
                <span class="exam-grid-fail">${mr.agreedMark}</span>
              <#else>
                ${mr.agreedMark}
              </#if>
            <#elseif mr.actualMark??>
              <#if mr.actualMark?number < passMark>
                <span class=exam-grid-fail">${mr.actualMark}</span>
              <#else>
                <span class=exam-grid-actual-mark">${mr.actualMark}</span>
              </#if>
            <#else>
              <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No marks set for Module registration">X</span>
            </#if>
          </td>
          <td class="assessment_details_col assessment_details_col1">
            <#if mr.agreedGrade??>
              ${mr.agreedGrade}
            <#elseif mr.actualGrade??>
              ${mr.actualGrade}
            <#else>
              <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No grade set for Module registration">X</span>
            </#if>
          </td>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>

  <#if mitigatingCircumstances?has_content>
    <div class="mitcircs_details">
      <h2>Mitigating circumstances</h2>
        <#list mitigatingCircumstances as submission>
          <div class="well">
            <h3>MIT-${submission.key}<#if submission.acute> &mdash; Acute mitigation already applied</#if></h3>

            <@components.detail label="Grading">
                ${submission.gradingCode} &mdash; ${submission.outcomeGrading.description} <@fmt.help_popover id="grading-${submission.key}" content="${submission.outcomeGrading.helpText}" />
            </@components.detail>

            <@components.detail label="Date approved">
              <#if submission.acute><@fmt.date date=submission.outcomesLastRecordedOn /><#else><@fmt.date date=submission.outcomesApprovedOn /></#if>
            </@components.detail>

            <#if submission.outcomeGrading.entryName != "Rejected">
              <#if submission.acute>
                <@components.detail label="Outcome" condensed=true>
                  <#if submission.acuteOutcome??>
                    ${submission.acuteOutcome.description}
                  <#else>
                    <span class="very-subtle">None</span>
                  </#if>
                </@components.detail>

                <#if submission.affectedAssessments?has_content>
                  <@components.detail "Affected assessments">
                  <ul class="list-unstyled">
                    <#list submission.affectedAssessments as assessment>
                      <#if ((assessment.acuteOutcome.entryName)!"") == ((submission.acuteOutcome.entryName)!"")>
                        <li><@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}</li>
                      </#if>
                    </#list>
                  </ul>
                  </@components.detail>
                </#if>
              <#else>
                <@components.detail "Recommendations to board">
                  <ul class="list-unstyled">
                    <#list submission.boardRecommendations as recommendation><#if !(recommendation.assessmentSpecific!false)>
                      <li>
                        <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                        (all assessments) <@fmt.help_popover id="${recommendation.entryName}-${submission.key}" content="${recommendation.helpText}" />
                      </li>
                    </#if></#list>

                    <#list submission.affectedAssessments as assessment>
                      <#if assessment.boardRecommendations?has_content>
                        <li>
                          <@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                          <ul>
                            <#list assessment.boardRecommendations as recommendation>
                              <li>
                                <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                                <@fmt.help_popover id="${recommendation.entryName}-${submission.key}" content="${recommendation.helpText}" />
                              </li>
                            </#list>
                          </ul>
                        </li>
                      </#if>
                    </#list>
                  </ul>
                </@components.detail>

                <@components.detail "Comments for board">
                  <#noautoesc>${submission.formattedBoardRecommendationComments}</#noautoesc>
                </@components.detail>
              </#if>
            </#if>

          </div>
        </#list>

    </div>
  </#if>
</div>