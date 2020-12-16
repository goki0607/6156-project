open Frontend

val target_flex : Syntax.cmd -> Syntax.symbl_tbl -> (Syntax.var, unit) Hashtbl.t

val target_anchor : Syntax.cmd -> Syntax.symbl_tbl -> (Syntax.var, unit) Hashtbl.t
