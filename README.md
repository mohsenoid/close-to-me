# Close To Me [![Actions Status](https://github.com/mohsenoid/CloseToMe/workflows/Android%20CI/badge.svg)](https://github.com/mohsenoid/CloseToMe/actions)
CloseToMe Android BLE Library

![CloseToMe Logo](/logo.png)

## How it works
This library uses Android BLE API to advertise a Beacon and scan for other Beacons at the same time.
This mechanism allows you to have an application that is aware of other clients getting close to the current device.
There can be many use cases for this library including contact tracing. 

![screenshot1](/screenshot1.png) ![screenshot2](/screenshot2.png)

## USAGE

Grab via Maven:
```xml
<dependency>
  <groupId>com.mohsenoid.close-to-me</groupId>
  <artifactId>close-to-me</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
```
or Gradle:
```groovy
implementation 'com.mohsenoid.close-to-me:close-to-me:1.0.3'
```

## License

Copyright 2021 Mohsen Mirhoseini

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
    

