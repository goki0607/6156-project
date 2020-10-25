{
(* lexing tokens for R programs *)

open Grammar
open Lexing
open Syntax

let lexical_error lexbuf _msg =
  let pos = lexeme_start_p lexbuf in 
  let pos_line = pos.pos_lnum in
  let pos_col = pos.pos_cnum - pos.pos_bol in
  let pos' = { pl = pos_line; pc = pos_col } in
  fail pos' "Syntax error"

let bool_of_string b = b = "true"
}

rule token = parse
    [' ' '\t']  { token lexbuf }
  | '\n'        { Lexing.new_line lexbuf; token lexbuf }
  | "int"       { INT }
  | "bool"      { BOOLEAN }
  | "if"        { IF }
  | "then"      { THEN }
  | "else"      { ELSE }
  | "while"     { WHILE }
  | "do"        { DO }
  | "anchor"    { ANCHOR }
  | "flex"      { FLEX }
  | "of"        { OF }
  | "_|_"       { BOT }
  | 'L'         { LOW }
  | 'M'         { MEDIUM }
  | 'H'         { HIGH }
  | 'T'         { TOP }
  | ("true" | "false") as boolean
                { BOOL (bool_of_string boolean) }
  | ['0'-'9']+('.'['0'-'9']*)?('e'('-')?['0'-'9'])? as num
                { NUM (int_of_string num) }
  | (['a'-'z'] | ['A'-'Z'])(['a'-'z'] | ['A'-'Z'] | ['0'-'9'])* as idt
                { IDENT idt }
  | "&&"        { AND }
  | "||"        { OR }
  | '!'         { NOT }
  | '+'         { PLUS }
  | '-'         { MINUS }
  | '*'         { TIMES }
  | '/'         { DIVIDE }
  | '<'         { LT }
  | "<="        { LTE }
  | '>'         { GT }
  | ">="        { GTE }
  | '='         { EQ }
  | "=="        { EQQ }
  | "!="        { NEQ }
  | '('         { LPAREN }
  | ')'         { RPAREN }
  | '{'         { LBRACKET }
  | '}'         { RBRACKET }
  | '['         { LSQBRACKET }
  | ']'         { RSQBRACKET }
  | ','         { COMMA }
  | ':'         { COLON }
  | ';'         { SEMICOLON }
  | eof         { END }
  | _ { lexical_error lexbuf None }
