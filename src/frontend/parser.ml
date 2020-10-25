(** Interface for parser *)

(** Parse from file given by path [file]. *)
let parse_from_file file =
  try
    let chan = open_in file in
    let lexbuf = Lexing.from_channel chan in
    let ast = Grammar.p Lexer.token lexbuf in
    close_in chan; ast
  with Sys_error _ ->
    failwith ("Could not find file " ^ file)

(** Parse from given string [s]. *)
let parse_from_string s =
  let lexbuf = Lexing.from_string s in
  Grammar.p Lexer.token lexbuf