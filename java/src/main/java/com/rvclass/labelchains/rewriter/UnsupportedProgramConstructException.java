package com.rvclass.labelchains.rewriter;

import java.lang.RuntimeException;

public class UnsupportedProgramConstructException extends RuntimeException {
  public UnsupportedProgramConstructException(String err) {
    super(err);
  }
  public UnsupportedProgramConstructException(String err, Throwable errt) {
    super(err, errt);
  }
}