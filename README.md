# BSPEntSpy - enhanced Entity Lump editor
BSPEntSpy is an updated version of **EntSpy** originaly made by Rof (http://www.bagthorpe.org/bob/cofrdrbob/).  

**Latest version** of BSPEntSpy is available to **download** here: https://github.com/jakgor471/BSPEntSpy/releases/tag/v1.33

## Features
* **Importing and exporting** entities
* **Patching from VMF**
* **FGD support** for SmartEdit like editing
* **Mass editing** for editing multiple entities at once (works as in Hammer Editor)
* **Undo / Redo** functionality for time-travelling and fixing mistakes
* **Enhanced filtering** and selection
* **Exporting / Importing embedded files** facilitating removal of broken cubemaps or packing files into the map file
* **Removing light information** for rebuilding the lighting using VRAD
* **Re-saving Static prop lump with different version** to fix 'stale map version' error

## Changes compared to the original version
Apart from features listed above the UI has been updated to use system
*Look and Feel*, instead of Java's default, dated and ugly *Metal* theme.  
Other changes include:
* switch from Tree entity display to a flat List
* complete overhaul of entity parameters edit panel (including separate tab for
editing flags)

Detailed description and instructions are included in **Help** menu tab in BSPEntSpy.

**Old version** to the left, **new version** to the right  
![comparison](https://github.com/jakgor471/BSPEntSpy/blob/main/images/image1.jpg?raw=true)

**Help panel**  
![comparison](https://github.com/jakgor471/BSPEntSpy/blob/main/images/image2.jpg?raw=true)

## Changelog
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
## Past versions
### Entspy v0.9 19/08/2015
* Updated BSP support by Envi https://gamebanana.com/tools/5876

### Entspy v0.9 XX/XX/2005
* Initial release by Rof I guess?

# Help
**Java Runtime Environment** is required to run the application.

## Patching from VMF HELP
Decompiling a map using software like **Valve Map Extractor** ("vmex") allows to edit the map in Hammer Editor. The process of decompilation however
does not necessarily yields a file valid enough to recompile unless the map is fixed manually. In such cases it is possible to use the patching functionality 
of BSPEntSpy.  
Patching is a process of matching the entities present in a VMF file to those present in BSP and replacing matched entities with those from VMF. 
Such approach allows to edit the entities in Hammer Editor and then transfer the changes directly into the BSP, no recompilation needed. 
Described process is not always accurate - entities in the BSP, unless named, have no means of identifying them other than the order in which 
they are defined, which matches that of a source (or decompiled) VMF file, therefore mismatches may occur.  

### Patching named entities
Once the source VMF file has been chosen (**File &gt; Patch from VMF**) the user will be prompted whether the patching should be done only for named entities 
or all entities. Patching named entities is safer, meaning it is less prone to mismatches. The entities will be matched by their class name, target name and by their 
order.  

### Patching all entities
While patching all entities, the entities will be matched only by class name and order. In case of a class mismatch the user will be prompted should the program 
continue patching and try to match the entities or abort the patching process. After the procedure user should inspect the entities for possible errors.  

### IMPORTANT
Patching process does not add any new entities, only replaces the one present in the BSP with those from the VMF file. It also does not delete any entities not present 
in the VMF file. Such edits need to be done manually.

## FGD files HELP
**FGD files** *(Game Definition File)* are used by Hammer Editor to display additional information about the entites. 
It is possible to load a single FGD file by clicking **File &gt; Load FGD File**. Once the FGD is loaded the Smart Edit mode can be enabled. 
In Smart Mode the entity parameter names are translated to their respective display names.  

Once the FGD file has been successfuly loaded the program will try to automaticaly load it on subsequent start. If remembered file cannot be 
opened or an error occurs during reading - it will be forgotten.

FGD files can be found in *&lt;SteamApps&gt;/common/GAME/bin* directories.

### Commonly used FGDs:
* Half-Life 2 and Episodes: *&lt;SteamApps&gt;/common/Half-Life 2/bin/halflife2.fgd*
* Garry's Mod: *&lt;SteamApps&gt;/common/GarrysMod/bin/garrysmod.fgd*
* Counter-Strike: Source: *&lt;SteamApps&gt;/common/Counter-Strike Source/bin/cstrike.fgd*

## Export / Import entities HELP
**Import**

Imports all entities from a specified file.

**Export selected**

Exports all selected entities into a specified file.

**Copy**

Copies all selected entities to a clipboard (see the format below).

**Paste**

Tries to parse the contents of a clipboard, if successful - pastes the parsed entities.

### File syntax HELP
Selected entities are outputted into a text file (.ent extension by default) or directly into the clipboard as a string, of which format is compliant with VMF format:

    entity  
    {  
     "key1" "value1"  
     "key2" "value2"  
     "key3" "value3"  
     ...  
    }  
    entity  
    {  
     "key1" "value1"  
     "key2" "value2"  
     "key3" "value3"  
     ...  
    }

Such file can be inspected and edited with regular text-editing software like Notepad++.

### Importing from VMF
Because of a compliance with VMF format it is possible to import a whole .VMF file. **Note** that some entities won't get imported due to them being declared as _internal entities_, that is - they get removed during VBSP step of a map compilation and should not exist in the final BSP entity lump.

Allowed brush entities will have their _model_ parameter set to _TODO: DEFINE IT!!!_. This parameter links the entity to a BSP brush model and since brush models are separate from entities one needs to specify them manually.