<#-- @ftlvariable name="queue" type="uk.ac.warwick.tabula.web.controllers.marks.MarksExportToSitsQueueCommand.Result" -->

<h1>SITS mark upload queue</h1>

<h2>Component marks</h2>

<p><@fmt.p queue.componentMarksNotEnabledForYear "mark is" "marks are" /> in the queue but mark upload is not enabled.</p>
<p><@fmt.p queue.componentMarksAgreedNotAllowed "agreed mark is" "agreed marks are" /> in the queue but the release date is in the future.</p>

<table class="table table-striped table-sortable">
  <thead>
    <tr>
      <th class="sortable" data-sortInitialOrder="asc" data-sorter="text">SPR code</th>
      <th class="sortable" data-sorter="text">Module code</th>
      <th class="sortable" data-sorter="text">Academic year</th>
      <th class="sortable" data-sorter="text">Occurrence</th>
      <th class="sortable" data-sorter="text">Sequence</th>
      <th class="sortable" data-sorter="text">Process</th>
      <th class="sortable" data-sorter="text">Resit sequence</th>
      <th class="sortable" data-sorter="number">Queued since</th>
    </tr>
  </thead>
  <tbody>
    <#list queue.componentMarksQueue as sprCodeStudent>
      <#assign sprCode = sprCodeStudent._1() />
      <#assign student = sprCodeStudent._2() />
      <tr>
        <td>${sprCode}</td>
        <td>${student.moduleCode}</td>
        <td>${student.academicYear.toString}</td>
        <td>${student.occurrence}</td>
        <td>${student.sequence}</td>
        <td>${student.assessmentType.sitsProcess}</td>
        <td>${student.resitSequence!}</td>
        <td data-sortby="${student.marks[0].updatedDate.millis}"><@fmt.date date=student.marks[0].updatedDate capitalise=false at=true relative=true /></td>
      </tr>
    </#list>
  </tbody>
</table>

<h2>Module marks</h2>

<p><@fmt.p queue.moduleMarksNotEnabledForYear "mark is" "marks are" /> in the queue but mark upload is not enabled.</p>
<p><@fmt.p queue.moduleMarksAgreedNotAllowed "agreed mark is" "agreed marks are" /> in the queue but the release date is in the future.</p>

<table class="table table-striped table-sortable">
  <thead>
    <tr>
      <th class="sortable" data-sortInitialOrder="asc" data-sorter="text">SPR code</th>
      <th class="sortable" data-sorter="text">Module code</th>
      <th class="sortable" data-sorter="text">Academic year</th>
      <th class="sortable" data-sorter="text">Occurrence</th>
      <th class="sortable" data-sorter="number">Queued since</th>
    </tr>
  </thead>
  <tbody>
    <#list queue.moduleMarksQueue as student>
      <tr>
        <td>${student.sprCode}</td>
        <td>${student.sitsModuleCode}</td>
        <td>${student.academicYear.toString}</td>
        <td>${student.occurrence}</td>
        <td data-sortby="${student.marks[0].updatedDate.millis}"><@fmt.date date=student.marks[0].updatedDate capitalise=false at=true relative=true /></td>
      </tr>
    </#list>
  </tbody>
</table>
