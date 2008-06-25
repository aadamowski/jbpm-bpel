package com.example.translator.spi;

import com.example.translator.Document;
import com.example.translator.TextNotTranslatable;

public interface Dictionary {

  public abstract String translate(String text) throws TextNotTranslatable;

  public abstract Document translate(Document document)
      throws TextNotTranslatable;
}