# deep-override

recursive object extending & overriding

```javascript
const override = require('deep-override');

const target = {
  Name: 'Snow',
  Age: 10,
  Addresses: [
    {
      House: 'Stark',
      Castle: 'WinterFell'
    }
  ]
};

const source = {
  Pet: 'Ghost',
  Addresses: [
    {
      House: 'Dragon'
    }
  ]
}

override(target, source);

console.log(target);
/*
{
  Name: 'Snow',
  Age: 10,
  Pet: 'Ghost',
  Addresses: [
    {
      House: 'Dragon',
      Castle: 'WinterFell'
    }
  ]
}
*/
```
