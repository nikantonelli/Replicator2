@ECHO off

FOR /F "tokens=3" %%A IN ('REG QUERY "HKLM\SOFTWARE\ODBC\ODBC.INI\PVE" /v server 2^>nul') DO (set dbserver=%%A)

set servernum=%dbserver:~-3%
set p_board='* Portfolio Kanban (%servernum%)'
set A_board='Avengers Kanban (%servernum%)'
echo updating settings/layout for board %p_board%
echo updating settings/layout for board %A_board%

java -jar replicator-1.1-jar-with-dependencies.jar -f replicator_config.xlsx -l -x 3 -n %servernum%
