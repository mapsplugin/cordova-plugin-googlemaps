$(document).on("deviceready", function() {
  var httpReq = null;
  
  $("li").hide();
  $("li:first").fadeIn();
  
  $("button").click(function() {
    $(this).parent().next("li").fadeIn();
  });
  
  $("#initBtn").click(function() {
    httpReq = new plugin.HttpRequest();
  });
  
  $("#httpReq_get").click(function() {
    httpReq.get("http://www.w3schools.com/jquery/demo_test.asp", function(status, data) {
      alert(data);
    });
  });
  
  $("#httpReq_post").click(function(event) {
    httpReq.post("http://www.w3schools.com/php/welcome.php", {
      name: $("input[name='name']").val(),
      email: $("input[name='email']").val()
    },function(err, data) {
      alert(data);
    });
  });
  
  
  $("#httpReq_getJSON").click(function() {
    httpReq.getJSON("http://www.w3schools.com/jquery/demo_ajax_json.js", function(status, data) {
      alert(JSON.stringify(data));
    });
  });
});