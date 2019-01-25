// jshint esversion: 6
const BaseClass = require('../BaseClass');

describe('[BaseClass]', () => {

  it('"instanceB.hello" should be "world"', () => {
    const instance = new BaseClass();
    instance.set('hello', 'world');
    expect(instance.get('hello')).toEqual('world');
  });

  it('"instanceB.anotherHello" should be the same as "instanceA.hello"', () => {
    const instanceA = new BaseClass();
    const instanceB = new BaseClass();
    instanceA.bindTo('hello', instanceB, 'anotherHello');
    instanceA.set('hello', 'world');
    expect(instanceA.get('hello')).toEqual('world');
    expect(instanceB.get('anotherHello')).toEqual('world');
  });


  it('"instance.one()" should receive "hello_changed" event.', (done) => {
    const instance = new BaseClass();
    instance.set('hello', 'world');

    instance.one('hello_changed', (prevValue, newValue) => {
      expect(prevValue).toEqual('world');
      expect(newValue).toEqual('Aloha');
      done();
    });
    instance.set('hello', 'Aloha');
  });

  it('"instance.hasEventListener(\'hello_changed\')"', () => {
    const instance = new BaseClass();

    instance.on('hello_changed', () => { });
    expect(instance.hasEventListener('hello_changed')).toEqual(true);

    instance.off('hello_changed');
    expect(instance.hasEventListener('hello_changed')).toEqual(false);
  });

  it('"instance.on()" should receive "hello_changed" event twice.', (done) => {
    (new Promise((resolve, reject) => {
      const instance = new BaseClass();

      const timer = setTimeout(reject, 50); // just in case

      let count = 0;
      const listener = (prevValue, newValue) => {
        count++;
        if (count === 2) {
          instance.off('hello_changed', listener);
          clearTimeout(timer);
          resolve(newValue);
        }
      };
      instance.on('hello_changed', listener);

      instance.set('hello', '你好');      // should receive
      instance.set('hello', 'こんにちは'); // should receive
      instance.set('hello', '안녕하세요');  // should not receive
    }))
    .then((answer) => {
      expect(answer).toBe('こんにちは');
      done();
    });
  });

  it('"instance.on() then .off()" should receive only one time "hello_changed" event.', (done) => {
    (new Promise((resolve, reject) => {
      const instance = new BaseClass();
      let count = 0;
      const listener = () => {
        count++;
        instance.off('hello_changed', listener);
      };
      instance.on('hello_changed', listener);

      setTimeout(() => {
        resolve(count);
      }, 10);

      for (let i = 0; i < 10; i++) {
        instance.set('hello', i);
      }
    }))
    .then((answer) => {
      expect(answer).toBe(1);
      done();
    });
  });

  it('"instance.off()" should remove all event listeners.', (done) => {
    (new Promise((resolve, reject) => {
      const instance = new BaseClass();
      let called = false;
      const dummyListener = () => {
        called = true;
      };

      instance.on('myEvent', dummyListener);
      instance.on('myEvent', dummyListener);
      instance.on('myEvent', dummyListener);
      instance.on('myEvent', dummyListener);
      instance.off('myEvent');
      instance.trigger('myEvent');

      setTimeout(() => {
        resolve(called);
      }, 10);

    }))
    .then((answer) => {
      expect(answer).toBe(false);
      done();
    });
  });

  it('"instance.onThrottled()" should receive event only 3 times.', (done) => {
    (new Promise((resolve) => {

      let eventCount = 0;
      const instance = new BaseClass();

      // eventListener should be involved 3 times.
      //   first time: at 50ms
      //   second time: at 100ms
      //   third time: at 150ms
      instance.onThrottled('myEvent', (receivedData) => {
        eventCount++;
      }, 50);

      // Trigger event 13 times (takes 130ms)
      let sendCount = 0;
      const triggerTimer = setInterval(() => {
        sendCount++;
        instance.trigger('myEvent', sendCount);
        if (sendCount === 13) {
          clearInterval(triggerTimer);
        }
      }, 10);

      // When 300ms, test should be complited.
      setTimeout(() => {
        resolve(eventCount);
      }, 300);

    }))
    .then((answer) => {
      expect(answer).toBe(3);
      done();
    });
  });

  it('"instance.trigger()" should fire a "myEvent" event.', (done) => {
    (new Promise((resolve, reject) => {

      const timer = setTimeout(reject, 10); // just in case
      const instance = new BaseClass();

      instance.on('myEvent', (...parameters) => {
        clearTimeout(timer);
        resolve(parameters);
      });

      instance.trigger('myEvent', 'data', 1);
    }))
    .then((receivedData) => {
      expect(receivedData[0]).toBe('data');
      expect(receivedData[1]).toBe(1);
      done();
    });
  });

  it('"instance.empty()" should delete all holded variables.', () => {
    const instance = new BaseClass();
    instance.set('hello1', 'world');
    instance.set('hello2', 'test');
    instance.empty();
    expect(instance.get('hello1')).toBe(undefined);
    expect(instance.get('hello2')).toBe(undefined);
  });

});
