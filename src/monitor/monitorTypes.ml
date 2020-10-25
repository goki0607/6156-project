open Frontend.Syntax

type 'a contents =
{
  value : 'a ;
  chains : Domain.t array ;
}
type m_int = (var, int contents) Hashtbl.t
type m_bool = (var, bool contents) Hashtbl.t

type enf =
{
  mutable cc : Domain.t Stack.t ;
  mutable bc : Domain.t ;
}
