// jshint esversion: 6
const BaseClass = require('../BaseClass');

describe('[BaseClass]', () => {
  const instanceA = new BaseClass();
  const instanceB = new BaseClass();

  it('"instanceA.hello" should be "world"', () => {
    instanceA.set('hello', 'world');
    expect(instanceA.get('hello')).toEqual('world');
  });

  it('"instanceB.anotherHello" should be the same as "instanceA.hello"', () => {
    instanceA.bindTo('hello', instanceB, 'anotherHello');
    expect(instanceB.get('anotherHello')).toEqual('world');
  });


  it('"instanceA.one()" should receive "hello_changed" event.', (done) => {
    instanceA.one('hello_changed', (prevValue, newValue) => {
      expect(prevValue).toEqual('world');
      expect(newValue).toEqual('Aloha');
      done();
    });
    instanceA.set('hello', 'Aloha');
  });

  it('"instanceA.hasEventListener(\'hello_changed\')"', () => {
    instanceA.on('hello_changed', () => { });
    expect(instanceA.hasEventListener('hello_changed')).toEqual(true);
    instanceA.off('hello_changed');
    expect(instanceA.hasEventListener('hello_changed')).toEqual(false);
  });

  it('"instanceA.on()" should receive "hello_changed" event twice.', (done) => {
    (new Promise((resolve, reject) => {
      const timer = setTimeout(reject, 100); // just in case

      let count = 0;
      const listener = (prevValue, newValue) => {
        count++;
        if (count === 2) {
          instanceA.off('hello_changed', listener);
          clearTimeout(timer);
          resolve(newValue);
        }
      };
      instanceA.on('hello_changed', listener);

      instanceA.set('hello', '你好');
      instanceA.set('hello', 'こんにちは');
    }))
    .then((answer) => {
      expect(answer).toBe('こんにちは');
      done();
    });
  });

  it('"instanceA.on() then .off()" should receive only one time "hello_changed" event.', (done) => {
    (new Promise((resolve, reject) => {
      let count = 0;
      const listener = () => {
        count++;
        instanceA.off('hello_changed', listener);
      };
      instanceA.on('hello_changed', listener);

      setTimeout(() => {
        resolve(count);
      }, 100);

      for (let i = 0; i < 100; i++) {
        instanceA.set('hello', i);
      }
    }))
    .then((answer) => {
      expect(answer).toBe(1);
      done();
    });
  });

  it('"instanceA.off()" should remove all event listeners.', (done) => {
    (new Promise((resolve, reject) => {
      let called = false;
      const dummyListener = () => {
        called = true;
      };

      instanceA.on('myEvent', dummyListener);
      instanceA.on('myEvent', dummyListener);
      instanceA.on('myEvent', dummyListener);
      instanceA.on('myEvent', dummyListener);
      instanceA.off('myEvent');
      instanceA.trigger('myEvent');

      setTimeout(() => {
        resolve(called);
      }, 100);

    }))
    .then((answer) => {
      expect(answer).toBe(false);
      done();
    });
  });

  it('"instanceA.onThrottled()" should receive only the last event.', (done) => {
    (new Promise((resolve) => {

      let eventCount = 0;

      // eventListener should be involved 3 times.
      instanceA.onThrottled('myEvent', (receivedData) => {
        eventCount++;
      }, 50);

      let sendCount = 0;
      const triggerTimer = setInterval(() => {
        sendCount++;
        instanceA.trigger('myEvent', sendCount);
        if (sendCount === 13) {
          clearInterval(triggerTimer);
        }
      }, 10);

      setTimeout(() => {
        resolve(eventCount);
      }, 200);

    }))
    .then((answer) => {
      expect(answer).toBe(3);
      done();
    });
  });

  it('"instanceA.trigger()" should fire a "myEvent" event.', (done) => {
    (new Promise((resolve, reject) => {

      const timer = setTimeout(reject, 100); // just in case

      instanceA.on('myEvent', (...parameters) => {
        clearTimeout(timer);
        resolve(parameters);
      });

      instanceA.trigger('myEvent', 'data', 1);
    }))
    .then((receivedData) => {
      expect(receivedData[0]).toBe('data');
      expect(receivedData[1]).toBe(1);
      done();
    });
  });

  it('"instanceA.empty()" should delete all holded variables.', () => {
    instanceA.set('hello1', 'world');
    instanceA.set('hello2', 'test');
    instanceA.empty();
    expect(instanceA.get('hello1')).toBe(undefined);
    expect(instanceA.get('hello2')).toBe(undefined);
  });

});
