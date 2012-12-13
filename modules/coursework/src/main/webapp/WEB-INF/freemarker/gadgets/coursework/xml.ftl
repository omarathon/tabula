<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Coursework submission">
    <Require feature="dynamic-height" />
    <OAuth>
      <Service name="coursework">
        <Access url="https://websignon.warwick.ac.uk/oauth/accessToken" method="GET" />
        <Request url="https://websignon.warwick.ac.uk/oauth/requestToken?scope=${oauthScope?url}" method="GET" />
        <Authorization url="https://websignon.warwick.ac.uk/oauth/authorise" />
      </Service>
    </OAuth>
  </ModulePrefs>
<Content type="html" view="home,profile,default"><![CDATA[  
<div id="main-content">
	 
<div id="approval"></div>
<div id="waiting"></div>
<div id="main"></div> 

</div>
<script type="text/javascript" src="https://start-test.warwick.ac.uk/static/gadgets/common/js/prototype-1.7.js"></script>
<script type="text/javascript" src="https://start-test.warwick.ac.uk/static/gadgets/common/js/popup.js"></script>
<script type="text/javascript" src="https://start-test.warwick.ac.uk/static/gadgets/common/js/warwick.js"></script>

<script type="text/javascript"> 
var prefs = new gadgets.Prefs();

var config = {
  dataUrl: 'https://java-monkey.warwick.ac.uk/coursework/api/gadget.html'
};

gadgets.util.registerOnLoadHandler(function(){

 // This is a standard function to handle the three OAuth divs. It will usually be the same for every gadget
var showOneSection = function(toshow) {
  var section;
	
  var sections = [ 'main', 'approval', 'waiting' ];
  for (var i=0; i < sections.length; ++i) {
    var s = sections[i];
    var el = $(s);
    if (s === toshow) {
      section = el;
      el.style.display = "block";
    } else {
      el.style.display = "none";
    }
  }

  if (section) {
    // Adjust the height of (stretch) the gadget
    adjustHeightWithConstraints();
		
    section.select('img').invoke('observe','load', function() {
      adjustHeightWithConstraints();
    });
  }
};

// The main function to load the data
var loadData = function() {

  // OAuthFetcher is specified in warwick.js
  OAuthFetcher.fetch(

    /* Specify where to get the data (the URL), the type of data, the name of the OAuth service and the method of fetching the data */

    config.dataUrl + '?ts=' + new Date().getTime(),
    gadgets.io.ContentType.HTML, 
    "coursework", 
    gadgets.io.MethodType.GET, 

    /* Generic OAuth config */

    showOneSection, 
    "main", 
    "waiting", 
    "approval", 

    /* The callback - a function called when the data is fetched */

    function(data) {
      // This is where we actually handle the data returned from the API
      // We would use this to populate the content of the "main" div
      document.getElementById('main').innerHTML = data;
      showOneSection('main');
    }, 

    /* Some boilerplate for the popup during OAuth authentication */

    "personalize", 
    "approvaldone"
  );

};

// When the gadget loads, call the "loadData" function
gadgets.util.registerOnLoadHandler(loadData);

});
</script>



]]></Content>
</Module>