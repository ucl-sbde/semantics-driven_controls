# Semantics-driven controls

## Getting Started 

To access use this repository, follow these steps:

1. Clone the repository
   ``` 
   git clone https://github.com/ucl-sbde/semantics-driven_controls.git 
   ```


# Mapping algorithms 

This repository also contains mapping algorithms to generate semantic models using Brick and SAREF concepts by mapping metadata from BIM and BAS sources. There are two mapping algorithms available, one for each metadata source (BIM and BAS). The BIM algorithm maps metadata from an IFC model to RDF, while the BAS algorithm maps metadata from a CSV point list to RDF. Once both models are generated separately, they can be merged based on common instances by uploading both wihtin a triple store (e.g., Fuseki).    

## Getting Started 

To access use the mapping algorithms, install the dependencies

Particulary for the IFCtoRDF converter, install the [gr.tuc.bim-library](https://github.com/kyriakos-katsigarakis/openmetrics/packages/875264) package, which is required to serialise and deserialise the IFC STEP file into objects.

## BIM-to-RDF converter



## BAS-to-RDF converter

The BAS-to-RDF conversion script is based on a simple CSV template following a structure of how some BAS point list can be exported from BAS tools. The template has a header row with columns containing human-readable labels. Each column represents classes and object or entity properties that apply to them. These include "Point name" describing measurement and control points (e.g., "writeable cmd label point onoff" referring to an On/Off command); "Data point identifier" for the external references that provide data reading and writing access to each of the points; and their corresponding "Device identifier" (e.g., serial numbers).

The process of mapping BAS data point information into RDF involves multiple steps. First, the tool iterates over the rows of the CSV, creating individual instances for each row (data point). Then, it adds the relevant metadata for each data point based on their respective columns, such as identifying the associated device/equipment. During this process, the algorithm identifies Brick and SAREF concepts corresponding to each data point using the description within the "Point name" column. 