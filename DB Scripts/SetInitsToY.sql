Update ip.epm_strategy
  set lk_board = (select structure_code from ip.structure where description =$(PortfolioBoard))
    where strategy_code in 
    (select structure_code from ip.structure where father_code in 
      (select structure_code from ip.structure where father_code = '16595')
    )
  

Update ip.epm_strat_custom
  set lk_sync_strat = 'Y'
   where strategy_code  in (
    select structure_code from ip.structure where father_code in (
      select structure_code from ip.structure where father_code = '16595'
      )
    )
 
 Go