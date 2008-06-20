package com.example.translator;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.example.translator.spi.Dictionary;
import com.example.translator.spi.DictionaryFactory;

public class DocumentTranslator_Impl implements DocumentTranslator, Remote {

  private static Map quotationRequests = new HashMap();

  private static final Log log = LogFactory.getLog(DocumentTranslator_Impl.class);

  public Document translate(TranslationRequest translationRequest)
      throws DictionaryNotAvailable, TextNotTranslatable, RemoteException {
    Document document = translationRequest.getDocument();
    Locale sourceLocale = new Locale(document.getHead().getLanguage());
    Locale targetLocale = new Locale(translationRequest.getTargetLanguage());
    DictionaryFactory dictionaryFactory = DictionaryFactory.getInstance(
        sourceLocale, targetLocale);
    Dictionary dictionary = dictionaryFactory.createDictionary(sourceLocale,
        targetLocale);
    return dictionary.translate(document);
  }

  public void quoteTranslation(QuotationRequest quotationRequest)
      throws RemoteException {
    String clientName = quotationRequest.getClientName();
    log.debug("received quotation request: clientName=" + clientName);
    quotationRequests.put(clientName, quotationRequest);
  }

  public StatusResponse getQuotationStatus(StatusRequest statusRequest)
      throws RemoteException {
    StatusResponse statusResponse = new StatusResponse();
    QuoteStatus quoteStatus;
    if (quotationRequests.containsKey(statusRequest.getClientName())) {
      quoteStatus = QuoteStatus.received;
    }
    else {
      quoteStatus = QuoteStatus.none;
    }
    statusResponse.setStatus(quoteStatus);
    return statusResponse;
  }
}
