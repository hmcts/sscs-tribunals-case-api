# properties-volume-nodejs
[![MIT license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](http://opensource.org/licenses/MIT)

This module is to incorporate the integration of the Azure key-vault flex volume to node properties.

# Usage
This module adds the properties volume entries into the configuration object from `'config'`
We use the default mount point of `/mnt/` volume, which happens exposes the key vault in [chart-nodejs](https://github.com/hmcts/chart-nodejs).

We use the last folder of the mount point, _secrets_, to map the properties into the configuration. 

Below is an example:
 ```json
{
  "secrets": {
    "VAULT": {
      "secretOne": "VALUE",
      "some-secret-two": "VALUE"
    },
    "VAULT2": {
      "secretOne": "VALUE",
      "some-secret-two": "VALUE"
    }
  }
}
```

**NOTE**
- The property **names** are not sanitised and are an exact copy from the file names on volume. This means when using 
  the hmcts/nodejs helm chart the property naming is exactly the same as those in the _key vault_.
  
- Application property **defaults** can be added to your application configuration for the `config` package using 
  the same object structure. 
   
   **e.g** To add a default for the property secrets.cmc.staff-email we would add the following to the configuration.
   
   in JSON:
   ```json 
   {
     "secrets": {
       "cmc": {
         "staff-email": "DEFAULT_EMAIL"
       }
     }
   }
   ```
   or in yaml
   ```yaml
   secrets:
      cmc:
        staff-email: DEFAULT_EMAIL
 
   ```
- If you have the need to add a **test** or add **multiple** property volumes in one application you can 
  override the volume mount point. To do this we can supply a value for the defaulted volume folder in the api
  i.e `addTo( config, {mountPoint:'some/other/folder/secrets'})`. 
  
- The **last folder name** is used as the prefix for the properties in the configuration 
  e.g. `/mnt/secrets` the properties start with `secrets`,  `/mnt/certs` the properties start with `certs`.
   
- If you mount volumes with the same last folder name e.g `/mnt/super/secrets` and `/mnt/silly/secrets`
  the properties will be fully merged together into the configuration object under `secrets` and the last property 
  volume that is merged in will override any properties with the same name.
 
## Quick start
```bash
$ yarn add @hmcts/properties-volume
```

### Typescript
```typescript
import * as config from 'config'
import * as propertiesVolume from '@hmcts/properties-volume'
propertiesVolume.addTo(config)
```

### Javascript
```javascript
config = require('@hmcts/properties-volume').addTo(require('config'))
```
 
### Options
The properties volume can be supplied with a couple of options via a _js_ like options object.
e.g.
```javascript
const config = require('@hmcts/properties-volume').addTo(require('config'),{mountPoint:'some/properties/mount/point'})
```

| Option | Description | Default | 
| ------ | ----------- | ------- |
| `mountPoint` | the folder where the properties volume exists. | `/mnt/secrets/`| 
| `failOnError` | Should this module throw an exception if mount does not exist or there is an error reading the properties | `false` | 

### Local access to vaults

You can configure the application to connect directly to the Azure Vaults specified in your application's Helm chart. *This is intended to be used locally, and not in production*.

This method uses your local Azure AD authentication so you will need to run `az login` before running your application.

```typescript
import * as config from 'config'
import { addTo, addFromAzureVault } from '@hmcts/properties-volume'

async function setupConfig() {
  if (process.env.NODE_ENV !== 'production') {
    await addFromAzureVault(config, { pathToHelmChart: 'charts/my-app/values.yaml' });
  } else {
    addTo(config);
  }
}
```

Note that this method is asynchronous and either needs to be awaited inside an async function or in a project with top level await enabled.

| Option | Description | Default | 
| ------ | ----------- | ------- |
| `pathToHelmChart` | path to the values.yaml file for the Helm chart. | `N/A`| 
| `env` | Used to calculate the vault name | `aat` | 

