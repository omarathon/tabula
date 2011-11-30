<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
<!doctype html>
<html lang="en-GB">
	<head>		  
		  <title>Coursework</title>
		
		  <meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" >
		  <meta charset="utf-8">
		  <meta name="robots" content="noindex,nofollow">

		  <@stylesheet "/static/css/concat6.css" />
		  <#include "_styles.ftl" />
		  <link rel="stylesheet" title="No Accessibility" href="/static/css/noaccessibility.css" type="text/css">
		  <link rel="alternate stylesheet" title="Show Accessibility" href="/static/css/showaccessibility.css" type="text/css">
		  
		  <!--[if lt IE 8]>
			  <link rel="stylesheet" href="/static/css/ielt8.css" type="text/css">
		  <![endif]-->
		  <!--[if lt IE 9]>
		  	<style type="text/css">
		 		#container {
					behavior: url(/static/css/pie.htc);
		 		}
		  	</style>
		  <![endif]-->
		  
		  <link rel="stylesheet" href="/static/css/fonts/standard.css" type="text/css">
		
		  <script type="text/javascript" src="/static/js/id6scripts.js"></script>
		  <script type="text/javascript" src="/static/libs/jquery-ui/js/jquery-ui-1.8.16.custom.min.js"></script>
		  <script type="text/javascript" src="/static/libs/anytime/anytimec.js"></script>
		  <script type="text/javascript" src="/static/js/main.js"></script>

	</head>
	<body class="horizontal-nav layout-100 coursework-page ${bodyClasses?default('')}">
		<div id="container">
			<!-- Change this to header-medium or header-large as necessary - large is for homepages only -->
			<div id="header" class="header-small" data-type="image">
			
				<div id="masthead" class="transparent"> <!-- optional: class="transparent" -->
					<div class="access-info">
      					<a href="#main-content" accesskey="c" title="Skip to content [c]">Skip to content</a>
      					<a href="#navigation" accesskey="n" title="Skip to navigation [n]">Skip to navigation</a>
    				</div>
				
					<!-- Remove this if non-branded -->
						<!-- The on-hover class here specifies that the links should only be displayed on hover -->
						<div id="warwick-logo-container" class="on-hover">
							<a id="warwick-logo-link" href="http://www.warwick.ac.uk" title="University of Warwick homepage">
								<img id="warwick-logo" src="<@url resource="/static/images/logo.png" />" alt="University of Warwick">
							</a>
			
							<div id="warwick-site-links">
								<ul>
									<li><a href="http://www2.warwick.ac.uk/study">Study</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/research">Research</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/business">Business</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/alumni">Alumni</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/newsandevents">News</a></li>
								</ul>
							</div>
						</div>
					<!-- End of removal if non-branded -->
					
					<div id="utility-container">
						<div id="utility-bar">
							<ul>
								<li>
								<#if user?? && user.loggedIn>
									Sign in as ${user.fullName}.
									<a class="sso-link" href="<@sso.logoutlink />">Sign out</a>
								<#else>
								    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
								</#if>
								</li>
							</ul>
						</div>
					
						<!-- Start removal here if non-branded -->
							<div id="search-container">
								<form action="http://search.warwick.ac.uk/website">
									<ul class="index-section">
										<li data-index-section="website" data-index-title="Warwick" title="" class="active">Search University of Warwick</li>
										<li data-index-section="people" data-index-title="People" data-go-type="people" title="">Search for people at Warwick</li>
										<li data-index-section="blogs" data-index-title="Blogs" data-go-disabled="true" title="">Search Warwick Blogs</li>
										<li data-index-section="exampapers" data-index-title="Exam papers" data-go-disabled="true" title="">Search past exam papers</li>
										<li data-index-section="website" data-index-file-format="audio/;video/" data-index-title="Video" data-go-disabled="true" title="">Search video</li>
										<li class="more-link">
											<a href="http://search.warwick.ac.uk/website" title="View more search options">More&hellip;</a>
											<ul id="search-index-menu" style="display: none"></ul>
										</li>
									</ul>
									<input autocomplete="off" id="search-box" class="large" placeholder="Type your search here" name="q" /><input alt="Search" id="search-button" type="image" src="/static/images/shim.gif" title="Click here to search" />
									<div id="search-suggestions" style="display: none"></div>
								</form>
							</div>
						<!-- End of removal if non-branded -->
					</div>
				</div>
				
				<div id="page-header">
					<div class="content">
						<div id="site-header-container">
							<h1 id="site-header">
								<span id="current-site-header"><a href="<@url page="/" />">Coursework submission</a></span>
							</h1>
							
							<h2 id="strapline">
								
							</h2>
						</div>
						
						<div id="custom-header">
							<!-- Enter any custom header content here -->
						</div>
					</div>
				</div>
			</div>
			
			<div id="navigation-and-content">
			
			
	<div id="navigation" class="horizontal">
	<div id="primary-navigation-wrapper">
		<div id="before-primary-navigation"></div>
		
		<div id="primary-navigation-container" class="fixed-width">
			<ul id="primary-navigation" class="cols-6">
				<li class="section selected-section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{THIS SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="#">{SECTION}</a>										
						</div>
						<!-- 
						<div class="description rendered-link-content">
							Description (Note, only on top level pages)
						</div>
						-->
					</div>
				</li>
			</ul>
		</div>
		
		<div id="after-primary-navigation"></div>
	</div>
	
</div>
			
				<div id="content-wrapper">					     
					<div id="main-content">
						<div id="page-title"> <!-- optional: class="site-root" (will hide the title using CSS) -->
			
							<h1>
								<span id="after-page-title"></span>
							</h1>
			
							<div id="page-title-bottom"></div>
						</div>
			
						<!-- column-1 and column-2 may not stick around as IDs - don't use them in a site design -->
						<div id="column-1"><div id="column-1-content">
						
						<@tiles.insertAttribute name="body" />
						
						</div></div>

						<div style="clear:both;"></div>
					</div>
				</div>
			
				<div style="clear:both;"></div>
			</div>
			
			<div style="clear:both;"></div>
			
			<div id="footer">
				<div class="content">
					<div id="custom-footer">
						<!-- Enter any custom footer content here (like contact details et al) -->
					</div>
					
					<div style="clear:both;"></div>
					
					<div id="common-footer">
						<div id="page-footer-elements" class="nofollow">
							<span class="footer-left"></span> <span class="footer-right"></span>
							
							<div style="clear:both;"></div>
						</div>
						
						<div id="footer-utility">
							<ul>
								<li id="sign-inout-link">
				          			<#if user?? && user.loggedIn>
										<a class="sso-link" href="<@sso.logoutlink />">Sign out</a>
									<#else>
									    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
									</#if>
			          			</li>
			          			<li class="spacer">|</li>
			          			<li id="copyright-link"><a href="http://go.warwick.ac.uk/terms" title="Copyright Statement">&copy; 2011</a></li>
			          			<li class="spacer">|</li>
			          			<li id="privacy-link"><a href="http://go.warwick.ac.uk/terms#privacy" title="Privacy statement">Privacy</a></li>
			          			<li class="spacer">|</li>
			          			<li id="accessibility-link"><a href="http://go.warwick.ac.uk/accessibility" title="Accessibility information [0]" accesskey="0">Accessibility</a></li>
		          			</ul>
		          				      					
	      					<div style="clear:both;"></div>
		          		</div>
					</div>
				</div>
			</div>
		</div>
	</body>
</html>