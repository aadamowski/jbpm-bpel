package com.example.translator.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.translator.DictionaryNotAvailable;
import com.example.translator.resource.ResourceDictionaryFactory;

public abstract class DictionaryFactory {

  private static List instances = new ArrayList();

  public abstract Dictionary createDictionary(Locale sourceLocale,
      Locale targetLocale);

  public abstract boolean acceptsLocales(Locale sourceLocale,
      Locale targetLocale);

  public static DictionaryFactory getInstance(Locale sourceLocale,
      Locale targetLocale) throws DictionaryNotAvailable {
    for (int i = 0, n = instances.size(); i < n; i++) {
      DictionaryFactory factory = (DictionaryFactory) instances.get(i);
      if (factory.acceptsLocales(sourceLocale, targetLocale)) {
        return factory;
      }
    }
    throw new DictionaryNotAvailable();
  }

  public static void registerInstance(DictionaryFactory instance) {
    instances.add(instance);
  }

  static {
    registerInstance(new ResourceDictionaryFactory());
  }
}