require 'net/https'
require 'uri'


module HttpHelper
  
  def response
    @response ||= fetch 
  end
  
  def fetch
    is_redirecting = true
    while is_redirecting
      uri = URI.parse(@url)
    
      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = true if uri.scheme == 'https'
      http.start {
        if uri.query then
           p="#{uri.path}?#{uri.query}"
        else
           p=uri.path
        end
      
        http.request_get(p, @headers) {|res|
  	      @response  = res
	      if @expected_responses[@url]
	        res.code.should == @expected_responses[@url].to_s
	      end
	      
	      if @follow_redirects && res.key?("location") then
  	   	    @url = res["location"]
	      else
	        is_redirecting = false
	      end
	      
	      res
	    }
      }
    end
    
    @response
  end
end

World(HttpHelper)

Before do
  @expected_responses = {}
  @follow_redirects = false
end