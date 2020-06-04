<div class="deptheader">
  <h1>Missing mark adjustment</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<p>
  If a module cohort is missing marks for this component because of <em>force majeure</em> (e.g. industrial action or COVID-19) then a
  Missing Marks Adjustment (MMA) will be made. This will normally scale the weighted average of the existing component marks of the module
  to reflect a mark out of 100. If a module leader requests to make a MMA in a different way and the Pre-exam Module Board approves it,
  there must be an explanation and rationale given for why the method of MMA was approved at the point of calculating module marks.
  All MMAs must be approved by the Pre-exam Module Board.
</p>

<p>
  Recording this assessment component as missing under MMA will record component marks for all students as <em>force majeure</em> (<code>FM</code>)
  under <a href="https://warwick.ac.uk/regulation41" target="_blank">University regulation 41</a> without a mark being recorded.
  When module marks are calculated in Tabula, components with a grade of <code>FM</code> will be excluded from the calculation.
</p>

<p>
  If all components for a module are recorded as missed under MMA and the a module cohort is missing a full module mark then a Missing
  Marks Adjustment (MMA) will be made at the level of the year credit at the Examination Board. For example, if a 15-credit module has
  no marks then the classification for that year will be calculated based upon 120-15 = 105 credits. All MMAs must be approved by the
  Pre-exam Module Board and ratified by the Examination Board. Tabula will calculate a module grade in this instance as <em>force majeure</em>
  (<code>FM</code>).
</p>

<#assign hasErrors = false />
<@spring.hasBindErrors name="command">
  <#assign hasErrors = errors.hasErrors() />
  <#if errors.hasErrors()>
    <div class="alert alert-danger">
      <#if errors.hasGlobalErrors()>
        <#list errors.globalErrors as e>
          <div><@spring.message message=e /></div>
        </#list>
      <#else>
        <div>See the errors below.</div>
      </#if>
    </div>
  </#if>
</@spring.hasBindErrors>

<#if !hasErrors>
  <#assign submitUrl><@routes.marks.assessmentcomponent_missingMarks assessmentComponent upstreamAssessmentGroup /></#assign>
  <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <button type="submit" class="btn btn-primary">Record this component as missed due to force majeure</button>
  </@f.form>
</#if>
