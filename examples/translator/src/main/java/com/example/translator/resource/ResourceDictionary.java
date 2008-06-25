package com.example.translator.resource;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.example.translator.Document;
import com.example.translator.DocumentBody;
import com.example.translator.DocumentHead;
import com.example.translator.TextNotTranslatable;
import com.example.translator.spi.Dictionary;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/20 12:28:17 $
 */
public class ResourceDictionary implements Dictionary {

  private final ResourceBundle bundle;

  public ResourceDictionary(ResourceBundle bundle) {
    this.bundle = bundle;
  }

  public String translate(String text) throws TextNotTranslatable {
    try {
      return bundle.getString(text);
    }
    catch (MissingResourceException e) {
      throw new TextNotTranslatable(text);
    }
  }

  public Document translate(Document document) throws TextNotTranslatable {
    DocumentHead transHead = new DocumentHead();
    transHead.setTitle(bundle.getString(document.getHead().getTitle()));
    transHead.setLanguage(bundle.getLocale().getLanguage());

    String[] paragraphs = document.getBody().getParagraph();
    String[] transParagraphs = new String[paragraphs.length];
    for (int i = 0; i < paragraphs.length; i++) {
      String paragraph = paragraphs[i];
      try {
        transParagraphs[i] = bundle.getString(paragraph);
      }
      catch (MissingResourceException e) {
        throw new TextNotTranslatable(paragraph);
      }
    }

    DocumentBody transBody = new DocumentBody();
    transBody.setParagraph(transParagraphs);

    Document targetDocument = new Document();
    targetDocument.setHead(transHead);
    targetDocument.setBody(transBody);

    return targetDocument;
  }
}
