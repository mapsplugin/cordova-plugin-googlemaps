document.addEventListener('deviceready', function() {
  cordova.exec(function() {
    //alert("OK");
  }, function() {
    alert("error");
  }, "touchTest", "exec", []);
  
  
  
  var div = document.getElementById("testArea");
  div.addEventListener("click", function() {
    alert("OK");
  });
}, false);
