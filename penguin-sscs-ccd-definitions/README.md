# SSCS CCD Definitions

# Local Development

To build a local version of the CCD Importer image:

    cd benefit
    docker build -t hmctspublic.azurecr.io/sscs/ccd-definition-importer-benefit:dev -f ../docker/importer.Dockerfile .
    
To generate a local AAT version of CCD def 

    cd ../
    ./bin/create-xlsx.sh benefit dev aat

To generate a local PROD version of CCD def

    ./bin/create-xlsx.sh benefit dev prod
    
To generate a shuttered CCD definition, run below script with corresponding parameters. 
Below command creates shuttered local PROD version of CCD def.

    ./bin/create-shuttered-xlsx.sh benefit dev prod

To generate a local version of CCD def similar to prod

    ./bin/create-xlsx.sh benefit dev local prod

You can then test loading this generated CCD def into your local environment version from sscs-docker project: Note:- local environment should have Python 3.0 or above version

    ./bin/ccd-import-definition.sh <your local directory path>/sscs-ccd-definitions/releases/CCD_SSCSDefinition_vdev_AAT.xlsx
    
# Pushing changes

    
* Raise a PR with the changes - Remember: check any new features have full CRUD access
* Open benefit/VERSION.yaml and modify the version number. This file will be used by the Azure Pipeline to create a docker importer image tagged with the specified version number.
* Update the benefit/data/sheets/CaseType.json and replace the right version for the field "Name": "SSCS Case v5.2.02_${CCD_DEF_E}"
* Get it approved and merged
* Pull latest master branch

Then, tag the master branch. Find the latest tag and bump by one. This tag does not relate to the version of either the benefit or bulkscan definitions. It is only the version
of this repository and does have any effect outside of that.
    
This little trick that will figure it out and tag automatically:

    git tag $(git describe --tags --abbrev=0 | awk -F. '{OFS="."}; {$NF+=1; print $0}')
    git push --tags
    
Run this command to manually bump the version number
NOTE: PLEASE DO NOT TRY TO KEEP THE CCD VERSION NUMBER AND THE TAGGED VERSION NUMBER IN SYNC AS IT CREATES CONFUSION AS THEY FREQUENTLY GET OUT OF SYNC

    git tag 1.0.18
    git push --tags
    
When a new tag is pushed, it will trigger an Azure Pipeline which will build the new benefit definition importer image and store it in the Azure Container Registry (ACR). In this case, the image will be called

    hmctspublic.azurecr.io/sscs/ccd-definition-importer-benefit:5.2.02
    
To create the AAT and PROD versions, move to the root of this repository, then run:

./bin/create-xlsx.sh benefit 5.2.02 aat
./bin/create-xlsx.sh benefit 5.2.02 prod

Put a message on the sscs-ccd slack channel with the new version

## File naming conventions

Filename structure is as follows: [major version].[minor version].[minor fix]_[environment] where
- Major version i.e. v1.x.x indicates major structural backwards incompatible changes to the definition, required to deliver new functionality or driven by changes to the definition structure as defined by the CCD team
- Minor version i.e. vx.1.x This should be incremented when minor, backwards compatible changes are pushed to prod.
- Patch version i.e. vx.x.1 indicates minor changes or fixes and should be used for the addition / removal of users, fields and updating callbacks.

## QA process

- Test the AAT version locally to make sure it doesn't break and change works as expected 
- If the change on Prod is different (e.g. if there is a <tab-name>-<feature_name>-prod.json> change), then also test the Prod version locally

After PO sign off:
- Upload AAT version onto AAT
- Run Tribunals pipeline to ensure no failures
- Run E2E test pipeline to ensure no failures

If all ok, create a ticket to get definition uploaded to Prod

*Note*: CRUD access can be changed in a future version to allow new features to be used, once all code is in-place


## Load a CCD definition to your local environment

Once version 5.2.02 appears in the ACR, you can run the following command to upload it to your local CCD Docker environment.

    ../bin/upload-ccd-definition-to-env.sh benefit 5.2.02 local
    
## Excel

If you prefer to work in Excel (usually for bigger changes), then create an Excel version

Move to the root of this repository. Find the latest version by looking at the VERSION.YAML file, then run:

    ./bin/create-xlsx.sh benefit 5.2.02 aat

The definition XLSXs will be created in the ./releases directory. (Takes a few seconds)

Make your changes to the relevant files

## Converting an XLSX definition to JSON

There are two SSCS CCD definition case types, benefit and bulkscan. Move into the directory of the one you need, e.g.

    cd benefit

    ../bin/xlsx2json.sh ~/Downloads/CCD_SSCSDefinition_v5.1.21_AAT.xlsx

### Sync with sscs-common

When you add fields to the sscs-common, you should also update the sscs-ccd-definitions so that the
definition file in AAT matches the java model in sscs-common, thereby avoiding validation failures.
We recommend that the AAT CCD definition to be generated through master branch

#Features

## Feature flagging
Any new features should be put behind feature flags in the definition:

- create a <tab-name>-<feature_name>-nonprod.json file in the relevant directory 
- if appropriate, also create a <tab-name>-<feature_name>-prod.json in the relevant directory, if you need to make a change to the prod version that you don't want to appear on the non prod version

Please ensure all new features are clearly put into their *own feature files*. This makes releases more straight forward as we know exactly what needs to be moved across as part of the release. 

##Variable substitution

A json2xlsx processor is able to replace variable placeholders defined in JSON definition files with values read from environment variables as long as variable name starts with CCD_DEF prefix.

For example CCD_DEF_BASE_URL=http://localhost environment variable gets injected into fragment of following CCD definition:

    [
      {
        "LiveFrom": "2017-01-01",
        "CaseTypeID": "Benefit",
        "ID": "createCase",
        "CallBackURLSubmittedEvent": "${CCD_DEF_BASE_URL}/callback"
      }
    ]
to become:

    [
      {
        "LiveFrom": "2017-01-01",
        "CaseTypeID": "Benefit",
        "ID": "createCase",
        "CallBackURLSubmittedEvent": "http://localhost/callback"
      }
    ]

## JSON fragments

A json2xlsx processor is able to read smaller JSON fragments with CCD definitions that helps splitting large definition files into smaller chunks. These fragments can be read from any level of nested directory as long as the top level directory corresponds to a valid sheet name.

For example large AuthorisationCaseField.json file presented below:

    [
      {
        "LiveFrom": "01/01/2017",
        "CaseTypeID": "Benefit",
        "CaseFieldID": "appeal",
        "UserRole": "caseworker-sscs-clerk",
        "CRUD": "CRU"
      },
      {
        "LiveFrom": "01/01/2017",
        "CaseTypeID": "Benefit",
        "CaseFieldID": "appeal",
        "UserRole": "caseworker-sscs-judge",
        "CRUD": "CRU"
      }
    ]
can be split into clerk.json file presented below:

    [
      {
        "LiveFrom": "01/01/2017",
        "CaseTypeID": "DRAFT",
        "CaseFieldID": "appeal",
        "UserRole": "caseworker-sscs-clerk",
        "CRUD": "CRU"
      }
    ]

and judge.json file presented below:

    [
      {
        "LiveFrom": "01/01/2017",
        "CaseTypeID": "DRAFT",
        "CaseFieldID": "appeal",
        "UserRole": "caseworker-sscs-judge",
        "CRUD": "CRU"
      }
    ]
located in AuthorisationCaseField directory that corresponds the XLS tab name.
