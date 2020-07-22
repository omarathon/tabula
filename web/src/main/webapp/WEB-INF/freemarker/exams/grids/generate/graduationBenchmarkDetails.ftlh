<#import "/WEB-INF/freemarker/mitcircs/mitcircs_components.ftlh" as components />

<#assign completedCatsCalculation>
    <#list breakdown.modules?keys as module>
        <#list mapGet(breakdown.modules, module) as component>
            <span
              class="use-tooltip"
              title="${component.assessmentComponent.moduleCode} - ${component.assessmentComponent.sequence}">${component.cats}</span> <#if component_has_next> + </#if>
        </#list> <#if module_has_next> + </#if>
    </#list>
</#assign>

<#assign yearWeighingNumerator>
    <#list breakdown.marksAndWeightingsPerYear?keys as year>
            (
              <#assign markTitle><#if year == command.yearOfStudy>Year ${year} weighted assessment mark<#else>Year ${year} average</#if></#assign>
              <span class="use-tooltip" title="${markTitle}">${mapGet(breakdown.marksAndWeightingsPerYear, year)._1()}</span> ×
              <span class="use-tooltip" title="Weighting of year ${year}">${mapGet(breakdown.marksAndWeightingsPerYear, year)._2()}</span>
              <#if year == command.yearOfStudy> × <span class="use-tooltip" title="Percentage of assessment taken in year ${year}">${breakdown.percentageOfAssessmentTakenDecimal}</span></#if>
            )
        <#if year_has_next> + </#if>
    </#list>
</#assign>

<#assign yearWeighingDenominator>
    <#list breakdown.marksAndWeightingsPerYear?keys as year>
        <#if year == command.yearOfStudy>
          (
            <span class="use-tooltip" title="Weighting of year ${year}">${mapGet(breakdown.marksAndWeightingsPerYear, year)._2()}</span>  ×
            <span class="use-tooltip" title="Percentage of assessment taken in ${year}">${breakdown.percentageOfAssessmentTakenDecimal}</span>
          )
        <#else>
          <span class="use-tooltip" title="Weighting of year ${year}">${mapGet(breakdown.marksAndWeightingsPerYear, year)._2()}</span>
        </#if>
        <#if year_has_next> + </#if>
    </#list>
</#assign>

<#macro list_component component module included><#compress>
          <tr>
            <td class="assessment_details_col assessment_details_col2">
              <div>${((component.assessmentComponent.moduleCode!"")?upper_case)}</div>
              <div>${component.assessmentComponent.module.name}</div>
            </td>
            <td class="assessment_details_col assessment_details_col2">
                ${component.assessmentComponent.sequence}
            </td>
            <td class="assessment_details_col assessment_details_col2">
                ${component.assessmentComponent.name}
            </td>
            <td class="assessment_details_col assessment_details_col2">
                ${component.assessmentComponent.assessmentType.name}
            </td>
            <td class="assessment_details_col assessment_details_col2">
                ${(module.cats!'-')}
            </td>
            <td class="assessment_details_col assessment_details_col2 <#if !component.member.deadline?has_content || component.member.deadline.isAfter(benchmarkCutoffDate)>alert alert-info</#if>">
                <#if component.member.deadline??>
                    <@fmt.date date=component.member.deadline includeTime=false relative=false shortMonth=true excludeCurrentYear=false />
                <#else>
                  <span class="very-subtle">Unknown</span>
                </#if>
            </td>
            <td class="assessment_details_col assessment_details_col2 <#if !component.member.firstDefinedMark?has_content>alert alert-info</#if>">
                ${component.member.firstDefinedMark!"X"}
            </td>
            <td class="assessment_details_col assessment_details_col2">
                ${component.member.firstDefinedGrade!}
            </td>
            <td class="assessment_details_col assessment_details_col2">
              <#if included>
                <i class="fa fa-check" title="Included"></i>
              <#else>
                <i class="fa fa-times" title="Excluded"></i>
              </#if>
            </td>
          </tr>
</#compress></#macro>
<style type="text/css" nonce="${nonce()}">
@media print {
	.exam-grid-preview .grid td {
		white-space: initial;
	}

	.table {
		max-width: 100%;
		width: 100%;
		table-layout: fixed;
	}
}
</style>
<div class="exam-grid-preview">
  <div class="deptheader">
    <h1>Graduation benchmark breakdown</h1>
    <#if department.examGridOptions.nameToShow.toString != 'none'>
      <h4 class="with-related">for ${member.fullName!}, ${member.universityId}</h4>
    <#else>
      <h4 class="with-related">for ${member.universityId}</h4>
    </#if>
  </div>

  <section>
    <h4>How is the weighted assessment mark calculated for the year?</h4>
    <p>Modules all have one or more forms of summative assessment that contribute to the overall module mark. We call each of these an ‘assessment component’ of the module and give each of them a weighting to determine how much they should influence the overall module mark. For example, a module might be assessed by an essay worth 40% and an exam worth 60%. It is the overall module mark which determines whether the module is passed which we would normally use to inform the degree classification.</p>
    <p>For the purpose of calculating weighted assessment marks for the graduation benchmark, we use the actual assessment component weightings within a module (see example below), rather than the overall module mark. This is therefore based upon the assessment components taken within the year, NOT the whole modules passed up to 13 March 2020.</p>

    <div class="panel panel-info">
      <div class="panel-heading"><div class="panel-title">Weighted assessment mark</div></div>
      <div class="panel-body">
        <div class="benchmark-breakdown-calculation">
          <div class="formula">
            <div class="numerator">
                <#list breakdown.modules?keys as module>
                    <#list mapGet(breakdown.modules, module) as component>
                      <span class="use-tooltip" title="${component.assessmentComponent.moduleCode} - ${component.assessmentComponent.sequence}">
                        (${component.cats} × ${component.member.firstDefinedMark!"X"})
                      </span>
                      <#if component_has_next> + </#if>
                    </#list><#if module_has_next> + </#if>
                </#list>
            </div>
            <div class="denominator">${completedCatsCalculation}</div>
          </div>
          <div>=</div>
          <div class="result">
              ${breakdown.weightedAssessmentMark}%
          </div>
        </div>
      </div>
    </div>
  </section>

  <section>
    <h4>How do we calculate the percentage of assessment taken in the year?</h4>
    <p>In order to determine the percentage of assessment taken in the year for the graduation benchmark calculation, you divide the total component mark weightings used in the weighted assessment mark calculation by the whole number of credits in the year (normally 120 credits, although this may be higher in some degree programmes).</p>
    <div class="panel panel-info">
      <div class="panel-heading"><div class="panel-title">Percentage of assessment taken</div></div>
      <div class="panel-body">
        <div class="benchmark-breakdown-calculation">
          <div class="formula">
            <div class="numerator">${completedCatsCalculation}</div>
            <div class="denominator">${breakdown.totalCats}</div>
          </div>
          <div>=</div>
          <div class="result">
              ${breakdown.percentageOfAssessmentTaken}% = ${breakdown.percentageOfAssessmentTakenDecimal}
          </div>
        </div>
      </div>
    </div>
  </section>

  <section>
    <h4>Calculating the Graduation Benchmark</h4>
    <p>For final year students, the safety net policy will determine a ‘graduation benchmark’ that will be calculated by determining the average weighted marks for assessments counting towards their degree submitted up to the end of term 2 (13/03/20). For clarity, this will be determined by considering assessments in previous years where these would be considered as part of the final degree calculation.</p>
    <p>The Graduation Benchmark has been calculated following the
      <a href="https://warwick.ac.uk/insite/coronavirus/staff/teaching/policyguidance/undergraduateintermediatefinal/graduationbenchmark-finalists/" target = "_blank">
        Teaching Continuity Policy Guidance for Final Year Undergraduate students</a>.
    </p>

    <div class="panel panel-info">
      <div class="panel-heading"><div class="panel-title">Graduation benchmark</div></div>
      <div class="panel-body">
        <#if breakdown.benchmarkErrors?has_content>
          <div class="alert alert-info">
            Unable to calculate the graduate benchmark.
            <ul>
              <#list breakdown.benchmarkErrors as benchmarkError>
                <li>${benchmarkError}</li>
              </#list>
              </ul>
          </div>
        <#else>
          <div class="benchmark-breakdown-calculation">
            <div class="formula">
              <div class="numerator">${yearWeighingNumerator}</div>
              <div class="denominator">${yearWeighingDenominator}</div>
            </div>
            <div>=</div>
            <div class="result">
              ${breakdown.graduationBenchmark}%
            </div>
          </div>
        </#if>
      </div>
    </div>
  </section>

  <#if !breakdown.benchmarkErrors?has_content>
    <section>
      <h4>Final year components</h4>
      <p>Components will only be included in the benchmark if there is a deadline of no later than
          <@fmt.date date=benchmarkCutoffDate includeTime=false relative=false shortMonth=true excludeCurrentYear=false />
        and a mark for this student.
      </p>
      <table class="table table-condensed grid">
        <thead>
        <tr class="assessment_componentdetails_header">
          <th>Module</th>
          <th>Sequence</th>
          <th>Name</th>
          <th>Type</th>
          <th>CATS</th>
          <th>Deadline</th>
          <th>Mark</th>
          <th>Grade</th>
          <th>Included in benchmark</th>
        </tr>
        </thead>
        <tbody>
        <#list breakdown.modules?keys as module>
            <#list mapGet(breakdown.modules, module) as component>
                <@list_component component, module, true/>
            </#list>
        </#list>
        <#list breakdown.excludedModules?keys as module>
            <#list mapGet(breakdown.excludedModules, module) as component>
                <@list_component component, module, false/>
            </#list>
        </#list>
        </tbody>
      </table>
    </section>
  </#if>
</div>
