const BaseClass = require('./BaseClass');

describe('BaseClass', () => {
  let instanceA = new BaseClass();
  let instanceB = new BaseClass();

  it('"instanceA.hello" should be "world"', () => {
    instanceA.set('hello', 'world');
    expect(instanceA.get('hello')).toEqual('world');
  });

  it('"instanceB.anotherHello" should be the same as "instanceA.hello"', () => {
    instanceA.bindTo('hello', instanceB, 'anotherHello');
    expect(instanceB.get('anotherHello')).toEqual('world');
  });

});
