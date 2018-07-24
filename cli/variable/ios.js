module.exports = function(req, res, next) {
  res.cyan('api key ->' + req.params.api_key);
  res.prompt();
};
