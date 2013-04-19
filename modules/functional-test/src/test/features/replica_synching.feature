Feature: Replica Synching
  As someone who runs many servers with no shared disk
  I want my files to be replicated by ID
  
  Scenario: Get the list of ids for a far-future date
  	When I request https://HOST/scheduling/sync/listFiles.json?start=1902016056220
  	Then the status code should be 200
  	And the Content-Type response header should be "application/json;charset=UTF-8"
  	And the Content-Length response header should be "84"
  	And the response body should include the text '"createdSince":1902016056220'
  	And the response body should include the text '"files":[]'
  	
  Scenario: Retrieve a file from the server
    When I request https://HOST/scheduling/sync/getFile?id=SYNC_TEST_FILE_ID&mac=SYNC_TEST_FILE_MAC
    Then the status code should be 200
    And the response body should include the text "This page has no content yet."
    
  Scenario: Retrieve a file from the server with no mac
    When I request https://HOST/scheduling/sync/getFile?id=SYNC_TEST_FILE_ID
    Then the status code should be 400
  
  Scenario: Retrieve a file from the server with an incorrect mac
    When I request https://HOST/scheduling/sync/getFile?id=SYNC_TEST_FILE_ID&mac=blahdeblah
    Then the status code should be 400