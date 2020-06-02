<div class="deptheader">
  <h1>Scaling</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<div class="row">
  <div class="col-md-4 col-md-push-8">
    <#if marks?has_content>
      <p>Marks for this assessment are shown on the graph. If marks are scaled, the Y axis values will change.</p>
      <script type="application/json" id="marks">${marks}</script>
      <canvas id="chart"></canvas>
    </#if>

    <div class="panel panel-default panel-body scaling-guidance">
      <h2>Scaling guidance</h2>

      <p>
        Before scaling assessment component marks, please read the
        <a target="_blank" href="https://warwick.ac.uk/insite/coronavirus/staff/teaching/marksandexamboards/guidance/marks/#scaling">university guidance for mark adjustment and scaling</a>.
      </p>
    </div>
  </div>

  <div class="col-md-8 col-md-pull-4">
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
        <li>A scaled pass mark for the assessment. If the pass mark is 40, a scaled pass mark of 35 will mean that students with a mark of 35 are scaled to 40.</li>
        <li>A scaled upper class mark for the assessment. A scaled upper class mark of 75 will mean that students with a mark of 75 are scaled down to 70.</li>
      </ul>

      <p>
        Marks are scaled linearly between 0 and the scaled pass mark, the scaled pass mark and the scaled upper class mark, and the scaled upper class mark
        to 100.
      </p>

      <p>
        Scaling down marks may lead to students failing who initially passed. If so, departments can either keep the pass mark at the same level and only lower
        the first class level when using the scaling tool, or make an academic judgement for each student affected.
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
            <@bs3form.labelled_form_group path="scaledPassMark" labelText="Scaled pass mark">
              <div class="input-group">
                <@f.input path="scaledPassMark" cssClass="form-control" autocomplete="off" type="number" min="1" max="100" />
                <div class="input-group-addon">%</div>
              </div>
            </@bs3form.labelled_form_group>
          </div>
          <div class="col-lg-4">
            <@bs3form.labelled_form_group path="scaledUpperClassMark" labelText="Scaled upper class mark">
              <div class="input-group">
                <@f.input path="scaledUpperClassMark" cssClass="form-control" autocomplete="off" type="number" min="41" max="100" />
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
