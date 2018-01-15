<#escape x as x?html>
<#import "*/group_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />

<@modal.wrapper cssClass="modal-lg">
	<@modal.header>
		<h3 class="modal-title">Students with small group event conflicts</h3>
	</@modal.header>
	<@modal.body>
		<#if groups?has_content>
			<#list groups?keys as group>
				<#assign students = mapGet(groups, group) />

				<div class="allocateStudentsToGroupsCommand well">
					<h4 class="name">${group.name}</h4>

					<ul class="drag-list unstyled">
						<#list students as student>
							<li class="student well well-sm">
								<div class="profile clearfix">
									<@fmt.member_photo student "tinythumbnail" false />
									<div class="name">
										<h6>${student.fullName} <@pl.profile_link student.universityId /></h6>
										${(student.mostSignificantCourseDetails.currentRoute.code?upper_case)!""} ${(student.mostSignificantCourseDetails.currentRoute.name)!""}<br />
										${student.shortDepartment!""}
									</div>
								</div>
							</li>
						</#list>
					</ul>
				</div>
			</#list>
		</#if>

		<#if allStudents?has_content>
			<p>
				<@fmt.bulk_email_students students=allStudents />
			</p>
		</#if>
	</@modal.body>
</@modal.wrapper>
</#escape>