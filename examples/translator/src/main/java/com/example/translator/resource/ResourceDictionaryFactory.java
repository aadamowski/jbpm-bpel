package com.example.translator.resource;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.example.translator.spi.Dictionary;
import com.example.translator.spi.DictionaryFactory;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/10/29 06:27:49 $
 */
public class ResourceDictionaryFactory extends DictionaryFactory {
	
	private static final Log log = LogFactory.getLog(ResourceDictionaryFactory.class);

  
  public boolean acceptsLocales(Locale sourceLocale, Locale targetLocale) {
    return getBundle(sourceLocale, targetLocale) != null;
  }
  
  
  public Dictionary createDictionary(Locale sourceLocale, Locale targetLocale) {
    return new ResourceDictionary(getBundle(sourceLocale, targetLocale));
  }
  
  protected ResourceBundle getBundle(Locale sourceLocale, Locale targetLocale) {
  	String sourceLanguage = sourceLocale.getLanguage();
  	log.debug("loading bundle: sourceLanguage=" + sourceLanguage + ", targetLocale=" + targetLocale);
  	ResourceBundle bundle;
    try {
    	bundle = ResourceBundle.getBundle(getBaseName(sourceLanguage), targetLocale);
    	String bundleLanguage = bundle.getLocale().getLanguage();
    	if (bundleLanguage.equals(targetLocale.getLanguage())) {
      	log.debug("loaded bundle: bundleLanguage=" + bundleLanguage);
    	}
    	else {
    	  bundle = null;
    	  log.debug("loaded bundle, but it does not correspond to the target locale: " +
    	  		"bundleLanguage=" + bundleLanguage);
    	}
    }
    catch (MissingResourceException e) {
      bundle = null;
    	log.debug("bundle not found", e);
    }
    return bundle;
  }
  
  protected String getBaseName(String sourceLanguage) {
    StringBuffer baseName = new StringBuffer(getClass().getName());
    baseName.setLength(baseName.lastIndexOf(".") + 1);
    baseName.append(sourceLanguage);
    return baseName.toString();
  }
}
