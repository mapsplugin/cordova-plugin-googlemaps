phonegap-http-request
=====================

###Installation
```bash
$> cordova plugin add plugin.http.request
```

### Create an instance
```js
var httpReq = new plugin.HttpRequest();
```

### HTTP / GET request
```js
httpReq.get("http://your.domain/get/", function(status, data) {
  alert(data);
});
```

### HTTP / GET request and JSON response
```js
httpReq.getJSON("http://your.domain/get/JSON/", function(status, data) {
  alert(JSON.stringify(data));
});
```

### HTTP / POST request
```js
httpReq.post("http://your.domain/post/", {
  name: $("input[name='name']").val(),
  email: $("input[name='email']").val()
},function(err, data) {
  alert(data);
});
```

### HTTP / POST request and JSON response
```js
httpReq.post("http://your.domain/post/JSON/", {
  name: $("input[name='name']").val(),
  email: $("input[name='email']").val()
},function(err, data) {
  alert(JSON.stringify(data));
});
```
