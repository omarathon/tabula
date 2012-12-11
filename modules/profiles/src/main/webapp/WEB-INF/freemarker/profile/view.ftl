<#escape x as x?html>

<#macro address address>
	<div class="vcard">
		<#if address.line1??>
			<p class="address">
				<span class="line1">${address.line1}</span>
				<#if address.line2??><br><span class="line2">${address.line2}</span></#if>
				<#if address.line3??><br><span class="line3">${address.line3}</span></#if>
				<#if address.line4??><br><span class="line4">${address.line4}</span></#if>
				<#if address.line5??><br><span class="line5">${address.line5}</span></#if>
				<#if address.postcode??><br><span class="postcode">${address.postcode}</span></#if>
			</p>
		</#if>
		<#if address.telephone??>
			<p class="tel">${address.telephone}</p>
		</#if>
	</div>
</#macro>

<style type="text/css">
/* Definition list reset */
#main-content dl { margin: 0; padding: 0; }
#main-content dt, #main-content dd { margin: 0; padding: 0; font-weight: normal; display: inline; }

.profile dt { float: left; clear: left; }
.profile dt:after { content: ':'; margin-right: 5px; }
.profile dd { float: left; clear: right; }

.personal-details div.photo { float: left; width: 198px; margin-right: 16px; }
.personal-details dl.col1   { float: left; width: 348px; margin-right: 16px; }
.personal-details dl.col2   { float: left; width: 348px; margin-right: 0; }

.personal-details header h1 { display: inline; }
</style>

<section class="search hero-unit">
	<h2>Search for a profile</h2>
	
	<@f.form method="post" action="${url('/search')}" commandName="searchProfilesCommand" cssClass="form-search">
		<@f.input path="query" cssClass="input-large search-query" />
		<button type="submit" class="btn">Search</btn>
	</@f.form>
</section>

<section class="profile">
	<section class="personal-details clearfix">
		<div class="photo">
			<img src="<@url page="/view/photo/${profile.universityId}.jpg" />" />
		</div>
		
		<header>
			<h1>${profile.fullName}</h1>
			<span class="description">${profile.groupName}<#if profile.route??>, ${route.name}</#if><#if profile.homeDepartment??>, ${profile.homeDepartment.name}</#if></span>
		</header>
		
		<dl class="col1 clearfix">
			<dt>Official name</dt>
			<dd>${profile.officialName}</dd>
			
			<dt>Preferred name</dt>
			<dd>${profile.fullName}</dd>
			
			<#if profile.gender??>
				<dt>Gender</dt>
				<dd>${profile.gender.description}</dd>
			</#if>
			
			<dt>Nationality</dt>
			<dd>${profile.nationality?default('Unknown')}</dd>
			
			<#if profile.dateOfBirth??>
				<dt>Date of birth</dt>
				<dd><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></dd>
			</#if>
			
			<#if profile.termtimeAddress??>
				<dt class="address">Term-time address</dt>
				<dd class="address"><@address profile.termtimeAddress /></dd>
			</#if>
			
			<#if profile.nextOfKins?size gt 0>
				<dt>Emergency contacts</dt>
				
				<#list profile.nextOfKins as kin>
					<dd>
						<#if kin.firstName?? && kin.lastName??>${kin.fullName}</#if>
						<#if kin.relationship??>(${kin.relationship})</#if>
					</dd>
				</#list>
			</#if>
		</dl>
		
		<dl class="col2 clearfix">			
			<#if profile.email??>
				<dt>Warwick email</dt>
				<dd>${profile.email}</dd>
			</#if>
			
			<#if profile.homeEmail??>
				<dt>Alternative email</dt>
				<dd>${profile.homeEmail}</dd>
			</#if>
			
			<#if profile.mobileNumber??>
				<dt>Mobile phone</dt>
				<dd>${profile.mobileNumber}</dd>
			</#if>
			
			<dt>University number</dt>
			<dd>${profile.universityId}</dd>
			
			<dt>IT code</dt>
			<dd>${profile.userId}</dd>
			
			<#if profile.homeAddress??>
				<dt class="address">Home address</dt>
				<dd class="address"><@address profile.homeAddress /></dd>
			</#if>
		</dl>
	</section>
</section>

<#if user.sysadmin>
	<@f.form method="post" action="${url('/view/' + profile.universityId + '/reimport')}">
		<button class="btn btn-large btn-danger" type="submit">Re-import details from ADS</button>
	</@f.form>
</#if>
</#escape>