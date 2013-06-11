<#import "../tutors/tutee_macros.ftl" as tutee_macros />

<div id="tutors">
<#escape x as x?html>

	<#assign _groupSet=smallGroup.groupSet />
	<#assign _module=smallGroup.groupSet.module />

    <h1>${_module.code?upper_case} ${_groupSet.name}, ${smallGroup.name}</h1>

	<@tutee_macros.table tutees=tutees is_relationship=false />

</#escape>
</div>