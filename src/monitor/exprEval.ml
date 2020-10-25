open Frontend.Syntax
open MonitorTypes

let rec evala mem_int k (a : aexpr) =
  match a with
  | Cst i -> i, Array.init k (fun _ -> Bot)
  | Var v -> 
    let entry = Hashtbl.find mem_int v in
    entry.value, entry.chains
  | Add (a1, a2) -> compute_op_arith mem_int k a1 a2 (+)
  | Sub (a1, a2) -> compute_op_arith mem_int k a1 a2 (-) 
  | Mul (a1, a2) ->
    let a1_val, a1_chains = evala mem_int k a1 in
    let a2_val, a2_chains = evala mem_int k a2 in
    let a3_val = a1_val * a2_val in
    let a3_chains = Array.map2 Domain.join a1_chains a2_chains in
    a3_val, a3_chains
  | Div (a1, a2) ->
    let a1_val, a1_chains = evala mem_int k a1 in
    let a2_val, a2_chains = evala mem_int k a2 in
    let a3_val = a1_val / a2_val in
    let a3_chains = Array.map2 Domain.join a1_chains a2_chains in
    a3_val, a3_chains
and compute_op_arith mem_int k a1 a2 op =
  let a1_val, a1_chains = evala mem_int k a1 in
  let a2_val, a2_chains = evala mem_int k a2 in
  let a3_val = op a1_val a2_val in
  let a3_chains = Array.map2 Domain.join a1_chains a2_chains in
  a3_val, a3_chains

let rec compute_op_cmp mem_int k a1 a2 op =
  let a1_val, a1_chains = evala mem_int k a1 in
  let a2_val, a2_chains = evala mem_int k a2 in
  let a3_val = op a1_val a2_val in
  let a3_chains = Array.map2 Domain.join a1_chains a2_chains in
  a3_val, a3_chains
and evalc mem_int k = function
  | Lt (a1, a2) -> compute_op_cmp mem_int k a1 a2 (<)
  | Le (a1, a2) -> compute_op_cmp mem_int k a1 a2 (<=)
  | Gt (a1, a2) -> compute_op_cmp mem_int k a1 a2 (>)
  | Ge (a1, a2) -> compute_op_cmp mem_int k a1 a2 (>=)
  | Eq (a1, a2) -> compute_op_cmp mem_int k a1 a2 (=)
  | Ne (a1, a2) -> compute_op_cmp mem_int k a1 a2 (<>)

let rec compute_op_bool (b1_val,b1_chains) (b2_val,b2_chains) op =
  let b3_val = op b1_val b2_val in
  let b3_chains = Array.map2 Domain.join b1_chains b2_chains in
  b3_val, b3_chains
and evalb mem_int mem_bool k = function
  | Cst (b) -> b, Array.init k (fun _ -> Bot)
  | Var (v) -> 
    let entry = Hashtbl.find mem_bool v in
    entry.value, entry.chains
  | Cmp (cmp) -> evalc mem_int k cmp
  | And (b1, b2) ->
    compute_op_bool (evalb mem_int mem_bool k b1) (evalb mem_int mem_bool k b2) (&&)
  | Or (b1, b2) ->
    compute_op_bool (evalb mem_int mem_bool k b1) (evalb mem_int mem_bool k b2) (||)
  | Not (b) ->
    let b'_val, b'_chains = evalb mem_int mem_bool k b in
    not b'_val, b'_chains
