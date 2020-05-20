<h1>Component Mark Upload Progress</h1>
<table class="table table-striped">
  <thead>
  <tr>
    <th rowspan="2">Department</th>
    <th rowspan="2">Number of assessment components</th>
    <th rowspan="2">Student count</th>
    <th rowspan="2">Students with marks</th>
    <th rowspan="2">Students without marks</th>
    <th>Percentage Complete</th>
    <th></th>
  </tr>
  </thead>
  <tbody>
  <#list result as resultItem>
    <tr>
      <td>${resultItem.department.name}</td>
      <td>${resultItem.componentMarkUploadProgress.assessmentComponentCount}</td>
      <td>${resultItem.componentMarkUploadProgress.totalStudentsCount}</td>
      <td>${resultItem.componentMarkUploadProgress.studentsWithMarksCount}</td>
      <td>${resultItem.componentMarkUploadProgress.totalStudentsCount - resultItem.componentMarkUploadProgress.studentsWithMarksCount}</td>
      <td>${resultItem.componentMarkUploadProgress.percentageComplete}</td>
      <td>
        <a class="btn btn-default btn-sm" href="<@routes.marks.assessmentcomponents resultItem.department academicYear />">View</a>
      </td>
    </tr>
  </#list>
  </tbody>
</table>
