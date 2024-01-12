## Overview

Not for public consumption. It is for a specific need within Planview to replicate demo environments

## Features
* Works from the TITLES of cards and not the IDs. This means if you have two cards, or two boards, of the same name, it might not pick up the one you want. Use LKUtils if you need it to import/export by ID.
* Will ignore card types (in a list in the config sheet column "Import Ignore") per replicate. This is useful for when there might be cards that come in from an external source, e.g. Portfolios that will need to be parented to child cards on other boards (see -c option)
* Cards that come in from Portfolios can be moved to an apprropiate lane by adding "Modify" rows into the Changes sheets on a name/title basis (see -c option)
* Can copy more than one board per run
* Command line options can used individually or together - there is not correlation between command line position and running order.
* Uses Excel to create a _template_ from which the process can be repeated.

## Command Line Options
Option | Argument | Description 
------ | -------- | -----------
-f | \<file\> | (String) Name of the Xlsx file to use for reading/writing
-x | \<level\> | (Integer) Output levels of debug statements: 0 = Errors, 1 = +Warnings, 2 = +Info, 3 = +Debug, 4 = +Network
-i |  | Run (i)mporter
-e |  | Run (e)xporter 
-r |  | (r)emake target boards by archiving old and adding new (if present, you can use -R first if needed)
-R |  | (R)emove target boards completely (delete)
-l |  | Replace (l)ayout on target board 
-t |  | Follow external links from (t)asktop integration to Jira or ADO and delete pair
-v |  | (BEWARE!) change le(v)els on target system to match source system. Used in conjunction with -l or -r, not independently.
-d |  | (d)elete all cards on the target boards
-c |  | Look for column "Import Ignore" for (c)omma separated list of types to treat in a special way on export/import
-O |  | Include _Older_ archived items during export
-A |  | Include _Attachments_ in export/import - these get placed in your current working directory 
-T |  | Include _Tasks_ in export/import
-C |  | Include  _Comments_ in export/import
-S |  | Include a comment in export containing link to original _Source_ (will not get imported if -C not used)
-g | \<group\> | Only import those in this group (marked in zlsx file)

## Example Option combinations
* To completely remove destination board(s) and remake it with a complete copy of cards, custom fields, custom icons, board levels, layout (not using Portfolios):<br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f ..\demo.xlsx -e -R -r -l -i
* To delete all cards on destination board(s) and recreate the cards from a previous export (not using Portfolios):<br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f ..\demo.xlsx -d -i
* To update the layout of destination board(s) with changes from their source (not using Portfolios):<br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f ..\demo.xlsx -l
* To create a brand new copy of a board (deleting the original) with all the cards, with attachments, tasks, comments, etc (not using Portfolios)<br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f "demo.xlsx" -x 3 -e -C -S -T -A -R -r -i
* To create a master file that generates instructions move Portfolios items to the right place and then import some more features/stories. Export from a completed (connected to Portfolios) board with: <br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f "demo.xlsx" -x 3 -e -c<br>
  Then after you have created the destination board and populated with the new Portfolios items, import and reconnect (the non-ignored items) with:<br>
  java -jar replicator\target\replicator-1.1-jar-with-dependencies.jar -f "demo.xlsx" -x 3 -i -c

## Setting up a demo environment that connects to Portfolios

The main reason for developing this variant (replicator) was to be able to create new demo environments, or reset existing ones, on demand.  The demo environments usually have to connect to Portfolios on the right, and then off to the left, on towards Jira or ADO.

This program can be used to just _replicate_ a board. within the same subscription, or between two completely independent subscriptions, if a connection to Portolios and/or Jira is not relevant. This might be useful if you have a new customer that just wants something to play with in a trial. As an SC, you can set up some 'useful' boards to have on-hand and when a prospect asks for something , you can replicate any number of boards over to their trial subscriptiopn from yours. All you need is an APIKey that can create boards in their system.

If you want to replicate a system that already is connected to Portfolios, there are a number of steps to go through to make sure that Portfolio resets itself correctly, initialises correctly, and reconnects to your target board correctly. There should be some bat scripts in this repo somewhere that will help.

The process is started off with a complete board that has the top level items already connected to Portfolio (e.g. initiatives and epics), all the children artifacts in AgilePlace, all the parent/child connections established. In effect, you need to create the whole environment from which you are going to make the _template_ replica of.

If the items from Portfolios have children that are on multiple boards, you need to create all the boards and then _export_ them in the correct sequence. The correct sequence means exporting the boards with the parents first and then those with the children. This allows the Excel spreadsheet to be created with the correct item connections

To create the initial _template_
1. Create master environment that has all items from Portfolios in the desired AgilePlace lanes, and all children. Portfolios drops all items into the default drop lane. This app will take those items whenDecide which item types are 
2.  Either:<br>
	Create Excel spreadsheet containing the Export of all the boards required and ignore Portfolios from now on (not using -c option)<br>
	Or:<br>
	Create a specific carefully crafted Excel spreadsheet by using the -c option on export after you put "Initiatives,Epics" into the Import Ignore column

To replicate:
1. Delete cards from the board that is connected to Portfolios<br>
	This will cause the Integration Hub to remove all the links to the cards on that board inside Portfolios (nice and tidy)
2. OPTIONAL: <br>
   Run a refresh on PLT DB from your preferred backup (that you created after deleting all links to AgilePlace from it)<br>
   Run the script to  set the LK_SYNC flags to yes for all Initiatives<br>
   Run the script to  set the LK_SYNC flags to yes for all Epics<br>
   These actions will bring the Initiatives and Epics over from Portfolios again (with the correct _new_ IDs).<br>
3. Run the import from the _template_ Excel spreadsheet you created above. Note: if you have done the optional steps above and want to relink up to Portfolios, the import command must use the -c option

## Parent/Child Relationships
 
The use of the spreadsheet allows the indirect logging of the parent/child relationships. This is useful when you don't yet know the Id of the cards in the destination board. A 'Modify' row in the Changes sheet will allow you to point a child to a parent item by using a FORMULA in the cell using a card title.
 
## OnScreen positioning and Indexes
 
The priority of a card is normally set as an index of a card with zero being at the top of the screen. The upshot of this is the importer may attempt to set the Index to some value that may not yet be valid (as all the cards have not yet been created) if you do them in the wrong order.

If you are manually creating the importer spreadsheet, you will need to bear this in mind. The exporter will re-order the indexes appropriately for you, instead of using the default order of: last card accessed comes first.

## Assigned Users on Import

If your destination system does not have the correct users set up, the users are ignored. The tool tries to match the "username" which is usually of emailAddress format.

The importer will take the spreadsheet field as a comma separated list of users.

## API Authorisation

AgilePlace uses the industry standard method of providing an API key by setting the Authorization header using Bearer. Generate an API key for each target system and place into the spreadsheet columns

When coming to do the deletion of artifacts that are referenced by the AP extrnalLink field, ADO does it differently. ADO requires you to have a username and password, but the password is set to the token (not your original password) that you have generated from the menu (icon _next_ to your avatar in the top right hand corner of the ADO UI). Place the token in the "ADO Token" column and the username in the "ADO User"
