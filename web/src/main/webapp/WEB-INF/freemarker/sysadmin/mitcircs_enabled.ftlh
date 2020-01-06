<#list unableToSubmit as unableToSubmitForDepartment>
    <div class="well" id="${unableToSubmitForDepartment.department.code}">
        <h2>${unableToSubmitForDepartment.department.name}</h2>
        <table class="table table-condensed">
            <thead>
                <tr>
                    <th>Route Code</th>
                    <th>Route name</th>
                    <th>Students unable to submit</th>
                </tr>
            </thead>
            <tbody>
                <#list unableToSubmitForDepartment.studentsByRoute?keys as route>
                    <tr>
                        <td>${route.code}</td>
                        <td>${route.name}</td>
                        <td>${mapGet(unableToSubmitForDepartment.studentsByRoute, route)?size}</td>
                    </tr>
                </#list>
                <tr>
                    <th colspan="2">Total:</th>
                    <td>${unableToSubmitForDepartment.totalStudents}</td>
                </tr>
            </tbody>
        </table>
    </div>
</#list>
