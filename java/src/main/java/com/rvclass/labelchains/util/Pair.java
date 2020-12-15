package com.rvclass.labelchains.util;

public class Pair<A,B> {
  private A fst;
  private B snd;

  public Pair(A fst, B snd) {
    this.fst = fst;
    this.snd = snd;
  }

  public A getFst() {
    return this.fst;
  }
  public B getSnd() {
    return this.snd;
  }

  public void setFst(A fst) {
    this.fst = fst;
  }
  public void setSnd(B snd) {
    this.snd = snd;
  }
}