module.exports = {
  moduleNameMapper: {
    '^cordova/(.*)$': '<rootDir>/node_modules/cordova-js/src/common/$1',
  },
  resolver: './jest-cordova-resolver.js',
  globals: {
    cordova: {}
  },
};
