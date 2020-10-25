type t = Frontend.Syntax.lbl

let compare = Frontend.Syntax.compare_lbl

let leq l1 l2 = compare l1 l2 <= 0

let join l1 l2 = if leq l1 l2 then l2 else l1
