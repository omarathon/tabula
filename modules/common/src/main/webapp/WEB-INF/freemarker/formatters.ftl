<#ftl strip_text=true />
<#escape x as x?html>

<#macro module_name module>
	<span class="mod-code">${module.code?upper_case}</span> <span class="mod-name">${module.name}</span>
</#macro>

<#macro assignment_name assignment>
	<@module_name assignment.module /> <span class="ass-name">${assignment.name}</span>
</#macro>

<#macro assignment_link assignment>
	<@module_name assignment.module />
	<a href="<@url page='/module/${assignment.module.code}/${assignment.id}/' />">
		<span class="ass-name">${assignment.name}</span>
	</a>
</#macro>

<#macro admin_assignment_link assignment>
	<@module_name assignment.module />
	<a href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/list' />">
		<span class="ass-name">${assignment.name}</span>
	</a>
</#macro>

<#macro route_name route>
	${route.code?upper_case} ${route.name}
</#macro>

<#macro date date at=false timezone=false seconds=false capitalise=true relative=true split=false shortMonth=false includeTime=true><#--
	--><#noescape><#--
		-->${dateBuilder(date, seconds, at, timezone, capitalise, relative, split, shortMonth, includeTime)}<#--
	--></#noescape><#--
--></#macro>

<#-- This macro assumes that time is a LocalTime -->
<#macro time time twentyFourHour=true seconds=false><#--
	--><#noescape><#--
		-->${timeBuilder(time, twentyFourHour, seconds)}<#--
	--></#noescape><#--
--></#macro>

<#-- Format week ranges for a SmallGroupEvent or MonitoringPoint -->
<#macro weekRanges object><#--
	--><#noescape><#--
		-->${weekRangesFormatter(object)}<#--
	--></#noescape><#--
--></#macro>

<#macro singleWeekFormat week academicYear dept><#--
	--><#noescape><#--
		-->${weekRangesFormatter(week, academicYear, dept)}<#--
	--></#noescape><#--
--></#macro>

<#macro weekRangeSelect event><#--
	--><#noescape><#--
		--><select name="week" class="weekSelector"><#--
			--><#list weekRangeSelectFormatter(event) as week><#--
				--><option value="${week.weekToDisplay}">week ${week.weekToStore}</option><#--
			--></#list><#--
		--></select><#--
	--></#noescape><#--
--></#macro>

<#macro p number singular plural="${singular}s" one="1" zero="0" shownumber=true><#--
--><#if shownumber><#if number=1>${one}<#elseif number=0>${zero}<#else>${number}</#if><#--
--> </#if><#if number=1>${singular}<#else>${plural}</#if></#macro>

<#macro interval start end=""><#--
--><#noescape><#--
	--><#if end?has_content>${intervalFormatter(start, end)}<#--
	--><#else>${intervalFormatter(start)}</#if><#--
--></#noescape><#--
--></#macro>

<#macro tense date future past><#if date.afterNow>${future}<#else>${past}</#if></#macro>

<#macro usergroup_summary ug>
<div class="usergroup-summary">
<#if ug.baseWebgroup??>
	Webgroup "${ug.baseWebgroup}" (${ug.baseWebgroupSize} members)
	<#if ug.includeUsers?size gt 0>
	+${ug.includeUsers?size} extra users
	</#if>
	<#if ug.excludeUsers?size gt 0>
	-${ug.excludeUsers?size} excluded users
	</#if>
<#else>
	<#if ug.includeUsers?size gt 0>
	${ug.includeUsers?size} users
	</#if>
</#if>
</div>
</#macro>

<#-- comma separated list of users by name -->
<#macro user_list_csv ids>
<@userlookup ids=ids>
	<#list returned_users?keys?sort as id>
		<#assign returned_user=returned_users[id] />
		<#if returned_user.foundUser>
			${returned_user.fullName}<#if id_has_next>,</#if>
		<#else>
			${id}<#if id_has_next>,</#if>
		</#if>
	</#list>
	</@userlookup>
</#macro>

<#macro profile_name profile>${profile.fullName}</#macro>
<#macro profile_description profile><span class="profile-description">${profile.description!""}</span></#macro>

<#macro nationality nationality><#--
--><#if nationality = 'British (ex. Channel Islands & Isle of Man)' || nationality = 'British [NO LONGER IN USE: change to 2826]' || nationality = 'NAT code 000 should be used for British'><#--
	--><span class="use-tooltip" data-placement="right" title="${nationality}">British</span><#--
--><#elseif nationality?starts_with('(Obsolete) Formerly ')><#--
	--><span class="use-tooltip" data-placement="right" title="${nationality}">${nationality?substring(20)}</span><#--
--><#else><#--
	-->${nationality}<#--
--></#if></#macro>

<#--	Macro for handling singleton & multiple attachments using a common filepath root

		attachments: either a FileAttachment, or Seq[FileAttachment] please
		page: the URL 'folder' path, for passing to a <@url> macro
		context: string to append to the 'download files' message
		zipFilename: filename (excluding extension) to use for zip downloads

		In the controller, ensure that there are @RequestMappings for the specified 'page', with suffixes of
		/attachment/{filename} for individual files, and
		/attachments/{zipfile}.zip (note trailing 's') for collated files
-->
<#macro download_attachments attachments page context="" zipFilename="download">
	<#if !page?ends_with("/")>
		<#-- ensure page is slash-terminated -->
		<#assign page = page + "/" />
	</#if>

	<#if !attachments?is_enumerable>
		<#-- assume it's a FileAttachment -->
		<#assign attachment = attachments />
	<#elseif attachments?size == 1>
		<#-- take the first and continue as above -->
		<#assign attachment = attachments?first />
	</#if>

	<#if attachment??>
		<#assign title>Download file ${attachment.name}<#if context?has_content> ${context}</#if></#assign>
		<div class="attachment">
			<@download_link filePath="${page}attachment/${attachment.name}" mimeType=attachment.mimeType title="${title}" text="Download ${attachment.name}" />
		</div>
	<#elseif attachments?size gt 1>
		<details class="attachment">
			<summary>
				<#assign title>Download a zip file of attachments<#if context?has_content> ${context}</#if></#assign>
				<@download_link filePath="${page}attachments/${zipFilename}.zip" mimeType="application/zip" title="${title}" text="Download files as zip" />
			</summary>

			<#list attachments as attachment>
				<#assign title>Download file ${attachment.name}<#if context?has_content> ${context}</#if></#assign>
				<div class="attachment">
					<@download_link filePath="${page}attachment/${attachment.name}" mimeType=attachment.mimeType title="${title}" text="Download ${attachment.name}" />
				</div>
			</#list>
		</details>
	</#if>
</#macro>

<#macro download_link filePath mimeType title="Download file" text="">
	<#if mimeType?matches("^audio/(mpeg|mp3|mp4|ogg|wav)$")>
		<audio controls="controls">
			<source src="<@url page='${filePath}'/>" type="${mimeType}" />
		</audio>
	<#elseif mimeType?matches("^video/(mp4|webm|ogv)$")>
		<video controls="controls">
			<source src="<@url page='${filePath}'/>" type="${mimeType}" />
		</video>
	</#if>
	<a class="long-running use-tooltip" href="<@url page='${filePath}'/>" title="${title}"><i class="icon-download"></i><#if text?has_content> ${text}</#if></a>
</#macro>

<#macro role_definition_description role_definition><#compress>
	${role_definition.description?lower_case}
</#compress></#macro>

<#macro course_description_for_heading studentCourseDetails>
		${(studentCourseDetails.course.name)!} (${(studentCourseDetails.course.code?upper_case)!})
</#macro>

<#macro course_description studentCourseDetails>
	<#if (studentCourseDetails.route.name) != (studentCourseDetails.course.name)>
		${(studentCourseDetails.course.name)!} (${(studentCourseDetails.course.code?upper_case)!})
	<#else>
		${(studentCourseDetails.course.code?upper_case)!}
	</#if>
</#macro>

<#macro lightbox_link enabled url>
	<#if enabled>
		<a href="${url}" rel="lightbox"><#nested /></a>
	<#else>
		<#nested />
	</#if>
</#macro>

<#macro member_photo member resize="thumbnail" lightbox=true >
	<div class="photo size-${resize}">
		<#if (member.universityId)??>
			<#local fullsize_img><@routes.photo member /></#local>
			<@lightbox_link lightbox fullsize_img>
			<img src="<@routes.photo member />?size=${resize}"/>
			</@lightbox_link>
		<#else>
			<img src="<@url resource="/static/images/no-photo${resize}.jpg" />" />
		</#if>
	</div>
</#macro>

<#macro relation_photo member relationship resize="thumbnail" lightbox=true >
	<div class="photo size-${resize}">
		<#if (member.universityId)??>
			<#local fullsize_img><@routes.relationshipPhoto profile relationship /></#local>
			<@lightbox_link lightbox fullsize_img>
			<img src="<@routes.relationshipPhoto profile relationship />?size=${resize}" />
			</@lightbox_link>
		<#else>
			<img src="<@url resource="/static/images/no-photo${resize}.jpg" />" />
		</#if>
	</div>
</#macro>

<#macro permission_button permission scope action_descr href="" tooltip="" classes="" type="a" data_attr="data-container=body" >
	<#local class></#local>
	<#local title></#local>

	<#if tooltip?has_content>
		<#local title>title='${tooltip}.'</#local>
		<#local classes='${classes} use-tooltip'?trim >
	</#if>

    <#if href??><#local href>href=${href}</#local></#if>

	<#if !can.do(permission,scope)>
		<#local classes='${classes} disabled use-tooltip'?trim >
		<#local title>title='You do not have permission to ${action_descr}.'</#local>
		<#local data_attr='${data_attr}'?replace("data-toggle=modal","") >
	</#if>



	<#if classes??><#local class>class='${classes}'</#local></#if>
	<${type} ${href} ${class} ${title} ${data_attr}><#noescape><#nested></#noescape></${type}>
</#macro>

<#macro bulk_email emails title subject>
	<#if emails?size gt 0 && emails?size lte 50>
		<a href="mailto:<#list emails as email>${email}<#if email_has_next>,</#if></#list><#if subject?? && subject?length gt 0>?subject=${subject?url}</#if>" class="btn">
			<i class="icon-envelope"></i> ${title}
		</a>
	</#if>
</#macro>

<#macro bulk_email_students students title="Email these students" subject="">
	<#assign emails = [] />
	<#list students as student>
		<#if student.email??>
			<#assign emails = emails + [student.email] />
		</#if>
	</#list>
	
	<@bulk_email emails title subject />
</#macro>

<#macro bulk_email_student_relationships relationships title="Email these students" subject="">
	<#assign emails = [] />
	<#list relationships as rel>
		<#if rel.studentMember?? && rel.studentMember.email??>
			<#assign emails = emails + [rel.studentMember.email] />
		</#if>
	</#list>
	
	<@bulk_email emails title subject />
</#macro>

</#escape>

