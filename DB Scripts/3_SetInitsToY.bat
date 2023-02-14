@ECHO off

FOR /F "tokens=3" %%A IN ('REG QUERY "HKLM\SOFTWARE\ODBC\ODBC.INI\PVE" /v server 2^>nul') DO (set dbserver=%%A)

set servernum=%dbserver:~-3%
set p_board='* Portfolio Kanban (%servernum%)'
echo Synch Initiatives to %p_board%

sqlcmd -S %dbserver% -i SetInitsToY.sql  -d plt -U sa -P PlanView1$Gre8 -v PortfolioBoard="%p_board%"