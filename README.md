# BSPEntSpy - enhanced Entity Lump editor :mag:
BSPEntSpy is an updated version of **EntSpy** originaly made by Rof (http://www.bagthorpe.org/bob/cofrdrbob/).  

**Latest version** of BSPEntSpy is available to **download** here: https://github.com/jakgor471/BSPEntSpy/releases/latest
![download count](https://img.shields.io/github/downloads/jakgor471/bspentspy/total.svg):fire:

## Features :heavy_check_mark:
* **Importing and exporting** entities
* **Patching from VMF**
* **FGD support** for SmartEdit like editing
* **Mass editing** for editing multiple entities at once (works as in Hammer Editor)
* **Undo / Redo** functionality for time-travelling and fixing mistakes
* **Enhanced filtering** and selection
* **Renaming** the map alongside with updating internal references and directories (for fixing broken cubemaps)
* **Exporting / Importing embedded files** facilitating removal of broken cubemaps or packing files into the map file
* **Removing light information** for rebuilding the lighting using VRAD
* **Re-saving Static prop lump with different version** to fix 'stale map version' error
* **Browsing, exporting and importing lightmaps**

## Showcase
![image2](./images/image1.jpg?raw=true)
| Console                                 | Lightmap viewer                         |
|-----------------------------------------|-----------------------------------------|
| ![image1](./images/image2.jpg?raw=true) | ![image3](./images/image3.jpg?raw=true) |

**Java Runtime Environment** is required to run the application.

## Latest changes :fire:
### BSPEntSpy v1.7 release 14/07/2026
* Added console window
* Added option to sort lightmaps by size, material (alphabetically) and number of styles
* Map info is printed to the console on map load

# Dependencies
* LZMA SDK (included in the source code) https://7-zip.org/sdk.html
* JSON-Java https://github.com/stleary/JSON-java