open Syntax

let redecl_err pos s =
  Format.sprintf "Cannot re-declare variable %s" s
  |> fail pos

let undecl_err pos s =
  Format.sprintf "Variable %s used without declaring" s
  |> fail pos

let undecl_assn_err pos s =
  Format.sprintf "Variable %s assigned to before declaring" s
  |> fail pos

let typ_arith_err pos s t1 t2 =
  Format.sprintf "Variable %s was declared to be of type %s but here it is being used as a %s" s t1 t2
  |> fail pos

let typ_assn_err pos t1 t2 =
  Format.sprintf "Assigning a %s expression to an %s variable" t1 t2
  |> fail pos

let mono_chain_err pos s =
  Format.sprintf "Label chain for flexible variable %s is not monotonically decreasing" s
  |> fail pos

let type_check prog =
  let typ_tbl : (var, typ) Hashtbl.t = Hashtbl.create 32 in 
  let rec check_aexp pos (aexpr : aexpr) =
    match aexpr with 
    | Cst _ -> ()
    | Var v ->
      begin match Hashtbl.find_opt typ_tbl v with
      | None -> undecl_err pos v
      | Some (Bool) -> typ_arith_err pos v "bool" "int"
      | Some (Int) -> ()
      end
    | Add (a1, a2) | Sub (a1, a2)
    | Mul (a1, a2) | Div (a1, a2) ->
      check_aexp pos a1; check_aexp pos a2
  in
  let check_acmp pos (acmp : acmp) =
    match acmp with 
    | Lt (a1, a2) | Le (a1, a2)
    | Gt (a1, a2) | Ge (a1, a2)
    | Eq (a1, a2) | Ne (a1, a2) ->
      check_aexp pos a1; check_aexp pos a2
  in
  let rec check_bexp pos (bexpr : bexpr) =
    match bexpr with 
    | Cst _ -> ()
    | Var v ->
      begin match Hashtbl.find_opt typ_tbl v with
      | None -> undecl_err pos v
      | Some (Int) -> typ_arith_err pos v "int" "bool"
      | Some (Bool) -> ()
      end
    | Cmp (acmp) -> check_acmp pos acmp
    | And (b1, b2) | Or (b1, b2) ->
      check_bexp pos b1; check_bexp pos b2
    | Not (b) ->
      check_bexp pos b
  in
  let rec checker = function
    | Skip (_) -> ()
    | DeclA (t, v, a, _, pos) ->
      if Hashtbl.mem typ_tbl v then redecl_err pos v
      else Hashtbl.add typ_tbl v t; check_aexp pos a;
    | DeclB (t,v, b, _, pos) ->
      if Hashtbl.mem typ_tbl v then redecl_err pos v
      else Hashtbl.add typ_tbl v t; check_bexp pos b
    | AssnA (v, a, pos) ->
      begin match Hashtbl.find_opt typ_tbl v with
      | None -> undecl_assn_err pos v
      | Some (Bool) -> typ_assn_err pos "int" "bool"
      | Some (Int) -> check_aexp pos a
      end
    | AssnB (v, b, pos) ->
      begin match Hashtbl.find_opt typ_tbl v with
      | None -> undecl_assn_err pos v
      | Some (Int) -> typ_assn_err pos "bool" "int"
      | Some (Bool) -> check_bexp pos b
      end
    | Ite (b, c1, c2, pos) ->
      check_bexp pos b; checker c1; checker c2
    | While (b, c, pos) ->
      check_bexp pos b; checker c
    | Cmds (cmds) -> List.iter checker cmds
  in
  List.iter checker prog

let rec ensure_mono pos v chain =
  match chain with
  | [] -> () 
  | [_] -> ()
  | l1 :: ((l2 :: _) as ls') ->
      if compare_lbl l1 l2 <= 0 then ensure_mono pos v ls'
      else mono_chain_err pos v

let lbl_check prog =
  let max_chain_len = ref 0 in
  let rec checker = function
    | Skip (_) -> ()
    | DeclA (_, v, _, Flex (chain), pos)
    | DeclB (_, v, _, Flex (chain), pos) ->
      ensure_mono pos v chain;
      max_chain_len := max !max_chain_len (List.length chain)
    | DeclA (_, _, _, _, _) | DeclB (_, _, _, _, _) ->
      max_chain_len := max !max_chain_len 1;
    | AssnA (_, _, _) | AssnB (_, _, _) -> ()
    | Ite (_, c1, c2, _) ->
      checker c1; checker c2
    | While (_, c, _) ->
      checker c
    | Cmds (cmds) ->
      List.iter checker cmds
  in
  List.iter checker prog;
  max_chain_len := !max_chain_len + 1;
  let rec extend_chain n acc =
    if n = 0 then List.rev acc
    else List.hd acc :: acc |> extend_chain (n-1)
  in
  let rec extend_chains = function
    | DeclA (t, v, a, Flex (chain), p) ->
      let extend_to = !max_chain_len - List.length chain in
      let chain' = extend_chain extend_to chain in 
      DeclA (t, v, a, Flex (chain'), p)
    | DeclB (t, v, b, Flex (chain), p) ->
      let extend_to = !max_chain_len - List.length chain in
      let chain' = extend_chain extend_to chain in 
      DeclB (t, v, b, Flex (chain'), p)
    | Ite (b, c1, c2, p) ->
      Ite (b, extend_chains c1, extend_chains c2, p)
    | While (b, c, p) ->
      While (b, extend_chains c, p)
    | Cmds (cmds) ->
      Cmds (List.map extend_chains cmds)
    | cmd -> cmd
  in
  let symb_tbl : (var, symbl_typ) Hashtbl.t = Hashtbl.create 32 in
  {
    prog = List.map extend_chains prog ;
    symbl_tbl = symb_tbl ;
    k = !max_chain_len ;
  }

let populate_symb_tbl prog =
  let rec collect_symbs = function
    | DeclA (_, v, _, typ, _) | DeclB (_, v, _, typ, _) ->
      begin match typ with
      | Flex _ -> Hashtbl.add prog.symbl_tbl v Flex
      | Anchor _ -> Hashtbl.add prog.symbl_tbl v Anchor
      end
    | Ite (_, c1, c2, _) ->
      collect_symbs c1; collect_symbs c2
    | While (_, c, _) ->
      collect_symbs c
    | Cmds (cmds) ->
      List.iter collect_symbs cmds
    | _ -> ()
  in
  List.iter collect_symbs prog.prog
