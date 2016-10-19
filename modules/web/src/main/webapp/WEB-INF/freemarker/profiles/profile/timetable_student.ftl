<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#assign isSelf = member.universityId == user.universityId />

<#if !isSelf>
	<details class="indent">
		<summary>${member.officialName}</summary>
		<#if member.userId??>
			${member.userId}<br/>
		</#if>
		<#if member.email??>
			<a href="mailto:${member.email}">${member.email}</a><br/>
		</#if>
		<#if member.phoneNumber??>
			${phoneNumberFormatter(member.phoneNumber)}<br/>
		</#if>
		<#if member.mobileNumber??>
			${phoneNumberFormatter(member.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<div class="pull-right">
	<a class="btn btn-primary" href="<@routes.profiles.department_timetables member.homeDepartment />">Show all timetables</a>
</div>
<h1 class="with-settings">Timetable</h1>

<#include "_timetable.ftl" />
</#escape>