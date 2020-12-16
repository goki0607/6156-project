%{ 
  (* header *)
  (* grammar rules for parsing R programs *)
  open Lexing
  open Syntax

  let mklocation s _e = {
    pl = s.pos_lnum;
    pc = s.pos_cnum - s.pos_bol;
  }
%}

/* declarations */

/* lexer tokens */
%token PLUS MINUS TIMES DIVIDE
%token LT LTE GT GTE EQQ NEQ
%token NOT AND OR
%token INT BOOLEAN
%token BOT LOW MEDIUM HIGH TOP
%token ANCHOR FLEX
%token LPAREN RPAREN LBRACKET RBRACKET LSQBRACKET RSQBRACKET
%token EQ OF IF THEN ELSE WHILE DO END
%token COMMA COLON SEMICOLON
%token <string> IDENT
%token <int>    NUM
%token <bool>   BOOL

/* associativity rules */
/* %nonassoc NO_ELSE  */
/* %nonassoc ELSE */
%left AND OR        /* evaluated fifth */
%left NOT           /* evaluated fourth */
%left PLUS MINUS    /* evaluated third */
%left TIMES DIVIDE  /* evaluated second */
%left UMINUS        /* evaluated first */       

%start p    /* the grammar entry point */
%type  <Syntax.lbl>      lbl    /* labels */    
%type  <Syntax.lbl list> lbls   /* label lists */    
%type  <Syntax.aexpr>    aexpr  /* arithmetic expressions */
%type  <Syntax.acmp>     acmp   /* arithmetic comparisons */
%type  <Syntax.bexpr>    bexpr  /* boolean expressions */
%type  <Syntax.cmd>      cmd    /* commands */
%type  <Syntax.cmd list> cmds   /* command lists */
%type  <Syntax.prog>     p      /* programs */

%% 
/* rules */

/* labels */
lbl:
  | BOT
      { Bot }
  | LOW
      { L }
  | MEDIUM 
      { M }
  | HIGH
      { H }
  | TOP
      { Top }

/* label lists */
lbls:
  | lbl = lbl
      { [lbl] }
  | lbltl = lbls COMMA lblhd = lbl
      { lblhd :: lbltl }

/* arithmetic expressions */
aexpr:
  | i = NUM
      { Cst i }
  | x = IDENT
      { Var x }
  | a1 = aexpr PLUS a2 = aexpr
      { Add (a1, a2) }
  | a1 = aexpr MINUS a2 = aexpr
      { Sub (a1, a2) }
  | MINUS a2 = aexpr
      { Sub (Cst (0), a2) } %prec UMINUS
  | a1 = aexpr TIMES a2 = aexpr
      { Mul (a1, a2) }
  | a1 = aexpr DIVIDE a2 = aexpr
      { Div (a1, a2) }
  | LPAREN a = aexpr RPAREN
      { a }

/* arithmetic comparisons */
acmp:
  | a1 = aexpr LT a2 = aexpr
      { Lt (a1, a2) }
  | a1 = aexpr LTE a2 = aexpr
      { Le (a1, a2) }
  | a1 = aexpr GT a2 = aexpr
      { Gt (a1, a2) }
  | a1 = aexpr GTE a2 = aexpr
      { Ge (a1, a2) }
  | a1 = aexpr EQQ a2 = aexpr
      { Eq (a1, a2) }
  | a1 = aexpr NEQ a2 = aexpr
      { Ne (a1, a2) }

/* boolean expressions */
bexpr:
  | b = BOOL
      { Cst b }
  | a = acmp
      { Cmp (a) }
  | NOT b = bexpr
      { Not (b) }
  | x1 = IDENT AND x2 = IDENT
      { And (Var (x1), Var (x2)) }
  | x1 = IDENT OR x2 = IDENT
      { Or (Var (x1), Var (x2)) }
  | x1 = IDENT AND b2 = bexpr
      { And (Var (x1), b2) }
  | x1 = IDENT OR b2 = bexpr
      { Or (Var (x1), b2) }
  | b1 = bexpr AND x2 = IDENT
      { And (b1, Var (x2)) }
  | b1 = bexpr OR x2 = IDENT
      { Or (b1, Var (x2)) }
  | b1 = bexpr AND b2 = bexpr
      { And (b1, b2) }
  | b1 = bexpr OR b2 = bexpr
      { Or (b1, b2) }
  | LPAREN b = bexpr RPAREN
      { b } 

/* commands */
cmd:
  | SEMICOLON
      { Skip (mklocation $startpos $endpos) }
  | INT v = IDENT COLON ANCHOR OF l = lbl EQ a = aexpr SEMICOLON
      { DeclA (Int, v, a, Anchor (l), mklocation $startpos $endpos) }
  | BOOLEAN v = IDENT COLON ANCHOR OF l = lbl EQ b = bexpr SEMICOLON
      { DeclB (Bool, v, b, Anchor (l), mklocation $startpos $endpos) }
  | INT v = IDENT COLON FLEX OF LSQBRACKET ls = lbls RSQBRACKET EQ a = aexpr SEMICOLON
      { DeclA (Int, v, a, Flex (ls), mklocation $startpos $endpos) }
  | BOOLEAN v = IDENT COLON FLEX OF LSQBRACKET ls = lbls RSQBRACKET EQ b = bexpr SEMICOLON
      { DeclB (Bool, v, b, Flex (ls), mklocation $startpos $endpos) }
  | INT v = IDENT COLON EQ a = aexpr SEMICOLON
      { DeclA (Int, v, a, Flex ([Bot]), mklocation $startpos $endpos) }
  | BOOLEAN v = IDENT COLON EQ b = bexpr SEMICOLON
      { DeclB (Bool, v, b, Flex ([Bot]), mklocation $startpos $endpos) }
  | v = IDENT COLON EQ a = aexpr SEMICOLON
      { AssnA (v, a, mklocation $startpos $endpos) }
  | v = IDENT COLON EQ b = bexpr SEMICOLON
      { AssnB (v, b, mklocation $startpos $endpos) }
  | IF LPAREN b = bexpr RPAREN THEN s1 = cmd ELSE s2 = cmd
      { Ite (b, s1, s2, mklocation $startpos $endpos) }
  | WHILE LPAREN b = bexpr RPAREN DO s = cmd 
      { While (b, s, mklocation $startpos $endpos) }
  | LBRACKET cmds = cmds RBRACKET
      { Cmds (List.rev cmds) }

/* command list */
cmds:
  | cmdtl = cmds cmdhd = cmd
      { cmdhd :: cmdtl (* statements in reverse order *) }
  | 
      { [] }

/* program */
p:
  | cmds = cmds END
      { List.rev cmds } 

%% (* trailer *)
