<div class="deptheader">
  <h1>Upload attendance from CSV</h1>
  <h5 class="with-related"><span class="muted">for</span> ${command.templatePoint.name}</h5>
</div>

<#macro student_state_table tuples>
  <table class="sortable table table-striped">
    <thead>
    <tr>
      <th class="sortable">First name</th>
      <th class="sortable">Last name</th>
      <th class="sortable">University ID</th>
      <th class="sortable">Attendance</th>
    </tr>
    </thead>
    <tbody>
    <#list tuples as tuple>
        <#assign student  = tuple._1() />
        <#assign state  = tuple._2() />
      <tr>
        <td>${student.firstName}</td>
        <td>${student.lastName}</td>
        <td>${student.universityId}</td>
        <td>${state.description}</td>
      </tr>
    </#list>
    </tbody>
  </table>
</#macro>

<#if updated?size &gt; 0>
  <div class="striped-section collapsible">
    <h4 class="section-title">
      <a class="collapse-trigger icon-container" href="#">
        <span class="emphasis">${updated?size}</span>
        <@fmt.p number=updated?size singular="student's" plural="students'" shownumber=false /> attendance will be updated
      </a>
    </h4>

    <div class="striped-section-contents">
      <div class="item-info">
        <@student_state_table updated />
      </div>
    </div>
  </div>
</#if>

<#if identical?size &gt; 0>
  <div class="striped-section collapsible">
    <h4 class="section-title">
      <a class="collapse-trigger icon-container" href="#">
        <span class="emphasis">${identical?size}</span>
          <@fmt.p number=identical?size singular="student's" plural="students'" shownumber=false /> attendance has not changed
      </a>
    </h4>

    <div class="striped-section-contents">
      <div class="item-info">
          <@student_state_table identical />
      </div>
    </div>
  </div>
</#if>

<#if invalid?size &gt; 0>
  <div class="striped-section collapsible">
    <h4 class="section-title">
      <a class="collapse-trigger icon-container" href="#">
        <span class="emphasis">${invalid?size}</span>
          <@fmt.p number=invalid?size singular="student" shownumber=false /> ignored
      </a>
    </h4>

    <div class="striped-section-contents">
      <div class="item-info">
        <p><@fmt.p number=invalid?size singular="student was" plural="students were" /> ignored as they were not valid.</p>
        <table class="sortable table table-striped">
          <thead>
          <tr>
            <th class="sortable">First name</th>
            <th class="sortable">Last name</th>
            <th class="sortable">University ID</th>
          </tr>
          </thead>
          <tbody>
          <#list invalid as tuple>
              <#assign student  = tuple._1() />
            <tr>
              <td>${student.firstName}</td>
              <td>${student.lastName}</td>
              <td>${student.universityId}</td>
            </tr>
          </#list>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</#if>

<@f.form method="post" modelAttribute="command">
  <#list valid as tuple>
    <#assign student  = tuple._1() />
    <#assign state  = tuple._2() />
    <input type="hidden" name="checkpointMap[${student.universityId}][${command.templatePoint.id}]" value="${state.dbValue}" />
  </#list>
  <div class="submit-buttons">
    <button type="submit" name="confirm" class="btn btn-primary">Confirm</button>
    <a class="btn btn-default dirty-check-ignore" href="<@routes.attendance.viewRecordPoints filterCommand.department filterCommand.academicYear command.templatePoint filterCommand.serializeFilter "" />">Cancel</a>
  </div>
</@f.form>
