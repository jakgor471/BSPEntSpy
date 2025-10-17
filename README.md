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

**Old version** to the left, **new version** to the right  
![comparison](https://github.com/jakgor471/BSPEntSpy/blob/main/images/image1.jpg?raw=true)

**Help panel**  
![comparison](https://github.com/jakgor471/BSPEntSpy/blob/main/images/image2.jpg?raw=true)

**Java Runtime Environment** is required to run the application.

## Changelog :fire:
### BSPEntSpy v1.67 release 17/10/2025
* Option to save map without changing the CRC checksum
* Lightmap browsing, exporting and importing
* Option to calculate map checksum
* Added safeguards
* Removed "unable to shade polygon normal ..." error
### BSPEntSpy v1.5 release 17/05/2025 ANNIVERSARY EDITION :birthday:
* Material edititing functionality
* Enhanced cubemap editing
* Multiple KeyValue edit, copy and paste
* Minor bug fixes
* Anniversary Credits-secret ;)
### BSPEntSpy v1.414R-A release 12/04/2025
* Added support for Quake 1 BSP
* Fixed bug with LZMA compressed maps (Team Fortress 2)
### BSPEntSpy v1.4R-A release 04/03/2025
* More flexible PAK file importing
* Functionality for renaming internal map structure (for fixing cubemaps etc.)
* Experimental features are no longer experimental :)
### BSPEntSpy v1.33b build 26/02/2025
* Drag and drop functionality
* Minor bug fixes
* R-A completed
### BSPEntSpy v1.33a build 17/11/2024 codename R-A
* Added option to filter entities by position and radius
* Update checking functionality
### BSPEntSpy v1.33 build 09/09/2024
* Minor and major bug fixes
* Added option to edit cubemaps (cubemapsize only)
* Added option to edit static props and re-save the Static Prop lump using different version (fix for 'stale map version' error)
### BSPEntSpy v1.32 build 02/09/2024
* Minor and major bug fixes
* Added option to export/import files embedded in Pak Lump
* Added option to remove light information (Lump 15 and 54)
### BSPEntSpy v1.3 build 12/07/2024
* Added support for GoldSrc maps
* Complete rewrite of BSP backend
* Support for LZMA compression when saving the map (only if the original map was compressed)
### BSPEntSpy v1.275 build 13/06/2024
* Added Entity list filtering
* Added an option to invert the selection
* Enhanced filtering, now (13/06/2024) with support for place holders
* Fixed "Go to" exception
### BSPEntSpy v1.2 build 06/06/2024
* Added support for new Team Fortress 2 BSP format (LZMA compressed entity lump)
### BSPEntSpy v1.1 build 30/05/2024
* Added Undo/Redo functionality
* Added Flag edition panel
* Added patching from VMF functionality
### BSPEntSpy v1.0 build 23/05/2024
* Changed the name to BSPEntSpy
* Added FGD support and Smart Edit
* Complete overhaul of Entity parameter editor
### Entspy v2.0 build 17/05/2024
* Changed the Tree entity display to a list
* Minor changes to the UI layout
* Changed the "Look and feel" to that matching the specific operating system
* Added an option to export and import entities
* Added a partial VMF support
* Extended the search possibilities
* Minor tweaks and clean-ups
* Added new icons

# Dependencies
* LZMA SDK (included in the source code) https://7-zip.org/sdk.html
* JSON-Java https://github.com/stleary/JSON-java