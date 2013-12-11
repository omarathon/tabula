Feature: Standard server headers
  As a security-conscious service owner
  I want to expose as little information about my server implementation as possible
  
  Scenario Outline: Requesting a page, no gzip header (Vary)
    When I request <url>
    Then the Server response header should be "Warwick"
    And the X-Powered-By response header header should be "X-Requested-With"
    And the Vary response header should not exist
    
    Examples:
      | url |
      | https://HOST/ |
      | https://HOST/coursework/ |
    
  Scenario: Request file from static
    Given there is a file at https://HOST/static/js/home.js.484115321229 
    When I request https://HOST/static/js/home.js.484115321229
    Then the Vary response header should be "Accept-Encoding,User-Agent,X-Requested-With"
    And the Content-Type response header should be "text/javascript"
    And the Content-Length response header should exist
    And the ETag response header should exist
    And the Server response header should be "Warwick"