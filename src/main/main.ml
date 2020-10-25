open Frontend

let _ =
  let ast = Parser.parse_from_file "prog" in
  Checking.type_check ast;
  let prog = Checking.lbl_check ast in
  Checking.populate_symb_tbl prog;
  Monitor.run_prog prog