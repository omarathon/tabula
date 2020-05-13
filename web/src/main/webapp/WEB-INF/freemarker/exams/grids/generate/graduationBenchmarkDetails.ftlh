<#import "/WEB-INF/freemarker/mitcircs/mitcircs_components.ftlh" as components />

<#assign completedCatsCalculation>
    <#list breakdown.modules?keys as module>
        <#list mapGet(breakdown.modules, module) as component>
            <span class="use-tooltip" title="${component.assessmentComponent.moduleCode} - ${component.assessmentComponent.sequence}">${component.cats}</span> <#if component_has_next> + </#if>
        </#list> <#if module_has_next> + </#if>
    </#list>
</#assign>

<div class="exam-grid-preview">
  <div class="deptheader">
    <#if department.examGridOptions.nameToShow.toString != 'none'>
      <h1>${member.fullName!}</h1>
      <h4 class="with-related">${member.universityId}</h4>
    <#else>
      <h1>${member.universityId}</h1>
    </#if>
  </div>

  <section>
    <h3>Weighted assessment mark</h3>
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
         ${breakdown.graduationBenchmark}%
      </div>
    </div>
  </section>
  <section>
    <h3>Percentage of assessment taken</h3>
    <div class="benchmark-breakdown-calculation">
      <div class="formula">
        <div class="numerator">${completedCatsCalculation}</div>
        <div class="denominator">${breakdown.totalCats}</div>
      </div>
      <div>=</div>
      <div class="result">
          ${breakdown.percentageOfAssessmentTaken}%
      </div>
    </div>
  </section>
</div>
