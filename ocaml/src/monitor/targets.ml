open Frontend.Syntax

let get_target_generic goal cmd symb_tbl =
  let vars : (var, unit) Hashtbl.t = Hashtbl.create 32 in
  let rec targets = function 
    | Skip _ -> ()
    | DeclA (_, v, _, _, _) | DeclB (_, v, _, _, _)
    | AssnA (v, _, _) | AssnB (v, _, _) ->
      if Hashtbl.find symb_tbl v = goal then
        Hashtbl.add vars v ()
    | Ite (_, c1, c2, _) ->
      targets c1; targets c2
    | While (_, c, _) ->
      targets c
    | Cmds (cmds) ->
      List.iter targets cmds
  in
  targets cmd;
  vars

let target_flex cmd symbl_tbl =
  get_target_generic Flex cmd symbl_tbl

let target_anchor cmd symbl_tbl =
  get_target_generic Anchor cmd symbl_tbl
