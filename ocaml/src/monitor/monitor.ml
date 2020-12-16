open Frontend.Syntax

open Domain
open ExprEval
open MonitorTypes
open Targets

let run_prog prog =
  let prog_actual = prog.prog in
  let k = prog.k in
  let symbl_tbl = prog.symbl_tbl in
  let mem_int : m_int = Hashtbl.create 32 in
  let mem_bool : m_bool = Hashtbl.create 32 in
  let evala = evala mem_int k in
  let evalb = evalb mem_int mem_bool k in
  let cc = Stack.create () in
  Stack.push Bot cc;
  let bc = ref Bot in
  let update_var cc bc var =
    if Hashtbl.mem mem_int var then
      let contents = Hashtbl.find mem_int var in
      let chains' = Array.map (fun wi -> join wi cc |> join bc) contents.chains in
      Hashtbl.replace mem_int var ({ contents with chains = chains' })
    else
      let contents = Hashtbl.find mem_bool var in
      let chains' = Array.map (fun wi -> join wi cc |> join bc) contents.chains in
      Hashtbl.replace mem_bool var ({ contents with chains = chains' })
  in
  let flex_update_int v a =
    let value, chains = evala a in
    let chains' = Array.map (fun wi -> Stack.top cc |> join wi |> join !bc) chains in
    Hashtbl.add mem_int v { value = value; chains = chains' }
  in
  let anchor_update_int v a =
    let contents = Hashtbl.find mem_int v in 
    let value, chains = evala a in
    let g_ae = leq (Stack.top cc |> join !bc |> join chains.(0)) contents.chains.(0) in
    bc := Stack.top cc |> join !bc |> join chains.(1);
    if g_ae then
      Hashtbl.replace mem_int v { contents with value = value }
    else
      failwith "monitor halt"
  in
  let flex_update_bool v b =
    let value, chains = evalb b in
    let chains' = Array.map (fun wi -> Stack.top cc |> join wi |> join !bc) chains in
    Hashtbl.add mem_bool v { value = value; chains = chains' }
  in
  let anchor_update_bool v b =
    let contents = Hashtbl.find mem_bool v in 
    let value, chains = evalb b in
    let g_ae = leq (Stack.top cc |> join !bc |> join chains.(0)) contents.chains.(0) in
    bc := Stack.top cc |> join !bc |> join chains.(1);
    if g_ae then
      Hashtbl.replace mem_bool v { contents with value = value }
    else
      failwith "monitor halt"
  in
  let rec run = function
    | Skip _ -> ()
    | DeclA (Int, v, a, Anchor (l), _) ->
      let init_value = 0 in
      let init_chains = Array.init k (fun _ -> Bot) in
      init_chains.(0) <- l;
      Hashtbl.add mem_int v { value = init_value; chains = init_chains };
      anchor_update_int v a
    | DeclA (Int, v, a, _, _) ->
      flex_update_int v a
    | DeclA (Bool, _, _, _, _) -> failwith "Should be impossible."
    | DeclB (Bool, v, b, Anchor (l), _) ->
      let init_value = false in
      let init_chains = Array.init k (fun _ -> Bot) in
      init_chains.(0) <- l;
      Hashtbl.add mem_bool v { value = init_value; chains = init_chains };
      anchor_update_bool v b
    | DeclB (Bool, v, b, _, _) ->
      flex_update_bool v b
    | DeclB (Int, _, _, _, _) -> failwith "Should be impossible."
    | AssnA (v, a, _) ->
      begin match Hashtbl.find symbl_tbl v with 
      | Flex -> flex_update_int v a
      | Anchor -> anchor_update_int v a
      end
    | AssnB (v, b, _) ->
      begin match Hashtbl.find symbl_tbl v with 
      | Flex -> flex_update_bool v b
      | Anchor -> anchor_update_bool v b
      end
    | Ite (b, c1, c2, _) ->
      let b_res, b_chain = evalb b in
      let cc' = Stack.top cc |> join b_chain.(0) in 
      Stack.push cc' cc;
      let w, a =
        if b_res then begin
          run c1;
          target_flex c2 symbl_tbl, target_anchor c2 symbl_tbl
        end else begin
          run c2;
          target_flex c1 symbl_tbl, target_anchor c1 symbl_tbl
        end
      in 
      Hashtbl.iter (fun v _ -> update_var cc' !bc v) w;
      if Hashtbl.length a <> 0 then
        bc := join !bc cc';
      ignore (Stack.pop cc);
    | While (b, c, _) ->
      let b_res, b_chain = evalb b in
      if b_res then
        let cc' = Stack.top cc |> join b_chain.(0) in
        Stack.push cc' cc;
        run c;
        ignore (Stack.pop cc)
      else begin
        let cc' = Stack.top cc |> join b_chain.(0) in
        let w, a = target_flex c symbl_tbl, target_anchor c symbl_tbl in
        Hashtbl.iter (fun v _ -> update_var cc' !bc v) w;
        if Hashtbl.length a <> 0 then
          bc := join !bc cc'
      end
    | Cmds (cmds) -> List.iter run cmds
  in
  List.iter run prog_actual
