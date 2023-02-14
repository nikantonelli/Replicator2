update ip.planning_entity
  set lk_board = (select structure_code from ip.structure where description =$(PortfolioBoard)) 
    where planning_code in (select planning_code from ip.epm_strat_work 
      where strategy_code  in (
        select structure_code from ip.structure where father_code in 
        (
          select structure_code from ip.structure where father_code = '16595'
       )))
 update ip.custom_data
      set lk_sync_work = 'Y',
           lk_sync_children ='Y'
        where planning_code in (
          select planning_code from ip.epm_strat_work 
              where strategy_code  in (
                select structure_code from ip.structure where father_code in 
                (
              select structure_code from ip.structure where father_code = '16595')
       )   )
       
 Go