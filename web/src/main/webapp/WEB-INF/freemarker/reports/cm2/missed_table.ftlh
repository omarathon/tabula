<table class="table table-striped table-sortable">
  <thead>
  <tr>
    <th class="sortable">First name</th>
    <th class="sortable">Last name</th>
    <th class="sortable">University ID</th>
    <th class="sortable">Student email</th>
    <th class="sortable">Module</th>
    <th class="sortable">Assignment</th>
    <th class="sortable">Extension</th>
    <th class="sortable">Working days late</th>
  </tr>
  </thead>
  <tbody>
  <#list report.entities as entity>
    <tr>
      <td>${entity.student.firstName}</td>
      <td>${entity.student.lastName}</td>
      <td>${entity.student.universityId}</td>
      <td>${entity.student.email}</td>
      <td>${entity.module.code?upper_case} ${entity.module.name}</td>
      <td>${entity.assignment.name}</td>
      <td>
        <#if entity.extension??>
          <@fmt.date entity.extension.expiryDate />
        <#else>
          None
        </#if>
      </td>
      <td>${entity.workingDaysLate}</td>
    </tr>
  </#list>
  </tbody>
</table>
