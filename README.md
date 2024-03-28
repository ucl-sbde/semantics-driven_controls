# Semantics-driven controls

This repository contains mapping algorithms to generate semantic models using Brick and SAREF concepts by mapping metadata from BIM and BAS sources. There are two mapping algorithms, one for each metadata source, using IFC as a source of BIM and a CSV point list as a source of BAS. Once both models are generated separately, they can be merged based on common instances by uploading both wihtin a triple store (e.g., Fuseki).    

## Getting Started 

To access use the mapping algorithms, follow these steps:

1. Clone the repository
   ``` 
   git clone https://github.com/ucl-sbde/semantics-driven_controls.git 
   ```

2. Install the library and dependencies

Particulary for the IFCtoRDF converter, install the [gr.tuc.bim-library](https://github.com/kyriakos-katsigarakis/openmetrics/packages/875264) package, which is required to serialise and deserialise the IFC STEP file into objects.

## IFCtoRDF converter

## CSVtoRDF converter
