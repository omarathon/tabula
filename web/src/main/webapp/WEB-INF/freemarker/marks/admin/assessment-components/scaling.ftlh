<div class="deptheader">
  <h1>Scaling</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<div class="row">
  <div class="col-md-4 col-md-push-8">
    <h2>Scaling guidance</h2>

    <p>
      Before scaling assessment component marks, please read the
      <a target="_blank" href="https://warwick.ac.uk/insite/coronavirus/staff/teaching/marksandexamboards/guidance/marks/#scaling">university guidance for mark adjustment and scaling</a>.
    </p>
  </div>

  <div class="col-md-8 col-md-pull-4">
  <#if marks?has_content>
    <script type="application/json" id="marks">${marks}</script>
    <canvas id="chart"></canvas>
  </#if>

    <#assign hasErrors = false />
    <@spring.hasBindErrors name="command">
      <#assign hasErrors = errors.hasGlobalErrors() />
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
      <#assign submitUrl><@routes.marks.assessmentcomponent_scaling assessmentComponent upstreamAssessmentGroup /></#assign>

      <p>
        In order to scale component marks the following information needs to be provided:
      </p>

      <ul>
        <li>The normal pass mark for the assessment (40 for undergraduate assessments or 50 for postgraduate assessments, except in very unusual circumstances)</li>
        <li>An adjustment factor, positive or negative, for the pass mark for the assessment. Positive numbers bring the pass mark down,
          negative numbers raise the pass mark.</li>
        <li>An adjustment factor, positive or negative, for the upper class mark (70) for the assessment. Positive numbers bring the mark required for upper class
          down, negative numbers raise the mark required.</li>
      </ul>

      <p>
        For example, a pass mark adjustment of <code>5</code> with a pass mark of <code>40</code> will scale a mark of <code>35</code> to be a pass mark of
        <code>40</code>. An upper class mark adjustment of <code>-5</code> will scale a mark of <code>75</code> to be a mark of <code>70</code>. Marks are scaled
        linearly between 0 and the adjusted pass mark threshold, the adjusted pass mark threshold and the adjusted upper class threshold, and the adjusted upper
        class threshold to 100.
      </p>

      <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
        <@f.hidden path="calculate" value="true" />

        <div class="row">
          <div class="col-lg-4">
            <@bs3form.labelled_form_group path="passMark" labelText="Pass mark">
              <@f.select path="passMark" cssClass="form-control">
                <@f.option value="40" />
                <@f.option value="50" />
              </@f.select>
            </@bs3form.labelled_form_group>
          </div>
          <div class="col-lg-4">
            <@bs3form.labelled_form_group path="passMarkAdjustment" labelText="Pass mark adjustment">
              <div class="input-group">
                <@f.input path="passMarkAdjustment" cssClass="form-control" autocomplete="off" type="number" min="-30" max="30" />
                <div class="input-group-addon">%</div>
              </div>
            </@bs3form.labelled_form_group>
          </div>
          <div class="col-lg-4">
            <@bs3form.labelled_form_group path="upperClassAdjustment" labelText="Upper class mark adjustment">
              <div class="input-group">
                <@f.input path="upperClassAdjustment" cssClass="form-control" autocomplete="off" type="number" min="-30" max="30" />
                <div class="input-group-addon">%</div>
              </div>
            </@bs3form.labelled_form_group>
          </div>
        </div>

        <div class="submit-buttons">
          <input type="submit" class="btn btn-primary" value="Preview changes" />
          <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Cancel</a>
        </div>
      </@f.form>
    </#if>
  </div>


</div>
<@script "/static/js/id7/scaling.js" />
