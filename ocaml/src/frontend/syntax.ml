(* Type definitions for the AST of R programs *)

type pos = { pl : int; pc : int }
let fail pos msg =
  failwith (Printf.sprintf "Error: line %d col %d: %s" pos.pl pos.pc msg)

type var = string
type lbl = Bot | L | M | H | Top [@@deriving ord]
type lbl_chain = lbl list

type lbl_typ =
  | Flex of lbl_chain
  | Anchor of lbl

type symbl_typ =
  | Flex
  | Anchor
type symbl_tbl = (var, symbl_typ) Hashtbl.t

type typ =
  | Int
  | Bool 

type aexpr =
  | Cst of int
  | Var of var
  | Add of aexpr * aexpr
  | Sub of aexpr * aexpr
  | Mul of aexpr * aexpr
  | Div of aexpr * aexpr

type acmp = 
  | Lt of aexpr * aexpr
  | Le of aexpr * aexpr
  | Gt of aexpr * aexpr
  | Ge of aexpr * aexpr
  | Eq of aexpr * aexpr
  | Ne of aexpr * aexpr

type bexpr =
  | Cst of bool
  | Var of var
  | Cmp of acmp
  | And of bexpr * bexpr
  | Or  of bexpr * bexpr
  | Not of bexpr

type cmd =
  | Skip  of pos
  | DeclA of typ * var * aexpr * lbl_typ * pos 
  | DeclB of typ * var * bexpr * lbl_typ * pos 
  | AssnA of var * aexpr * pos 
  | AssnB of var * bexpr * pos
  | Ite   of bexpr * cmd * cmd * pos 
  | While of bexpr * cmd * pos 
  | Cmds  of cmd list

type prog = cmd list 

type prog_with_info =
  {
    prog      : prog ;
    mutable symbl_tbl : symbl_tbl ;
    k         : int ;
  }
