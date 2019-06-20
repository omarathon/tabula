Further details related to this request:
<#--

--><#if moduleManagers?has_content>

Module Manager<#if (moduleManagers?size > 1)>s</#if>:

<#list moduleManagers as manager><#if manager.foundUser>
* ${manager.fullName} (${manager.warwickId!}) <#if manager.email??>(${manager.email})</#if>
</#if></#list></#if>

Student contact details:

* Mobile number: ${(student.mobileNumber)!"Not available"}
* Telephone number: ${(student.phoneNumber)!"Not available"}
* Email address: ${(student.email)!"Not available"}
<#--

--><#if relationships?has_content>
<#list relationships?keys as key><#if relationships[key]?has_content>

${key}<#if (relationships[key]?size > 1)>s</#if>:

<#list relationships[key] as agent>
* ${agent.agentName} (${agent.agent})<#if agent.agentMember?? && agent.agentMember.description?has_content>, ${agent.agentMember.description}</#if>
</#list></#if></#list></#if><#--
--><#if scdRoute?has_content>

Student course details:

* Route: ${(scdRoute.name)!} (${(scdRoute.code?upper_case)!})
* Course: <#if (scdRoute.name) != (scdCourse.name)>${(scdCourse.name)!} (${(scdCourse.code?upper_case)!})<#else>${(scdCourse.code?upper_case)!}</#if>
* Intended award and type: ${(scdAward.name)!} (${(scdRoute.degreeType.toString)!})
</#if>