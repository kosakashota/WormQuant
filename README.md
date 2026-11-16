# WormQuant

An ImageJ plugin that batch-measures TIFF images in a specified directory and exports the results to a CSV file along with filenames and parameters extracted from Leica metadata XML.

## Usage

Launch from the Fiji / ImageJ menu via Plugins > WormQuant > Quantification Helper.

*To be added*

## Installation

### Build by your own

``` 
Bash
./gradlew jar         # Build the .jar only (output to build/libs/)
./gradlew copyToFiji  # Build and copy to the Fiji plugins folder (specify destination via fiji.dir in gradle.properties)
./gradlew runFiji     # Build, copy, and launch Fiji
./gradlew runPlugin   # Launch ImageJ directly with this plugin
```