# university-maps

Gets tweets and facebook posts about universities,
does sentiment analysis based on the text and returns the data
in a csv file with data that can be presented in Google Maps Javascript Project

## Installation

Download from https://github.com/lqiu96/University-Maps.

## Usage

Simply run the jar will put the respective data in an output folder
under the names 'TwitterOutput.csv' and 'FacebookOutput.csv'

    $ java -jar university-maps-0.1.0-standalone.jar [args]

## Options

The args will determine where the data will be stored.
* First argument- Twitter data
* Second argument- Facebook data
* Third argument- Number of results
Any argument after that will be ignored

## Examples

```java
//There are two static functions which outputs the data to two separate files
//Two parameters- Default results is 10
//Three parameters- Choose the number of results (Min: 1)

UniversityMaps.getData(/*Location for Twitter Data*/,/*Location for Facebook Data*/);
UniversityMaps.getData(/*Location for Twitter Data*/,/*Location for Facebook Data*/, /*Num results*/);
```

### Bugs

...

## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
