<#import "../relationships/student_macros.ftl" as student_macros />

<div id="relationships">
<#escape x as x?html>

	<#assign _groupSet=smallGroup.groupSet />
	<#assign _module=smallGroup.groupSet.module />

    <h1>${_module.code?upper_case} ${_groupSet.name}, ${smallGroup.name}</h1>

	<@student_macros.table students=tutees is_relationship=false />
	
	<p>
		<@fmt.bulk_email_students students=tutees subject="${_module.code?upper_case} ${_groupSet.name}, ${smallGroup.name}" />
	</p>

</#escape>
</div>