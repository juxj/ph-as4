/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4.messaging.encrypt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.attachment.WSS4JAttachmentCallbackHandler;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;

public class EncryptionCreator
{
  private final AS4CryptoFactory m_aCryptoFactory;

  public EncryptionCreator (@Nonnull final AS4CryptoFactory aCryptoFactory)
  {
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    m_aCryptoFactory = aCryptoFactory;
  }

  @Nonnull
  public Document encryptSoapBodyPayload (@Nonnull final ESOAPVersion eSOAPVersion,
                                          @Nonnull final Document aDoc,
                                          final boolean bMustUnderstand,
                                          @Nonnull final ECryptoAlgorithmCrypt eCryptAlgo) throws Exception
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");
    ValueEnforcer.notNull (eCryptAlgo, "CryptAlgo");

    final CryptoProperties aCryptoProps = m_aCryptoFactory.getCryptoProperties ();

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (aSecHeader);
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSymmetricEncAlgorithm (eCryptAlgo.getAlgorithmURI ());
    aBuilder.setUserInfo (aCryptoProps.getKeyAlias (), aCryptoProps.getKeyPassword ());
    aBuilder.getParts ().add (new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content"));
    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));
    return aBuilder.build (m_aCryptoFactory.getCrypto ());
  }

  @Nonnull
  public MimeMessage encryptMimeMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                         @Nonnull final Document aDoc,
                                         final boolean bMustUnderstand,
                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                         @Nonnull final AS4ResourceManager aResMgr,
                                         @Nonnull final ECryptoAlgorithmCrypt eCryptAlgo) throws WSSecurityException,
                                                                                          MessagingException
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    ValueEnforcer.notNull (aDoc, "XMLDoc");

    final CryptoProperties aCryptoProps = m_aCryptoFactory.getCryptoProperties ();

    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();

    final WSSecEncrypt aBuilder = new WSSecEncrypt (aSecHeader);
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (eCryptAlgo.getAlgorithmURI ());
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (aCryptoProps.getKeyAlias (), aCryptoProps.getKeyPassword ());
    // "cid:Attachments" is a predefined constant
    aBuilder.getParts ().add (new WSEncryptionPart (MessageHelperMethods.PREFIX_CID + "Attachments", "Content"));

    WSS4JAttachmentCallbackHandler aAttachmentCallbackHandler = null;
    if (CollectionHelper.isNotEmpty (aAttachments))
    {
      aAttachmentCallbackHandler = new WSS4JAttachmentCallbackHandler (aAttachments, aResMgr);
      aBuilder.setAttachmentCallbackHandler (aAttachmentCallbackHandler);
    }

    final Attr aMustUnderstand = aSecHeader.getSecurityHeaderElement ()
                                           .getAttributeNodeNS (eSOAPVersion.getNamespaceURI (), "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));

    // Main sign and/or encrypt
    final Document aEncryptedDoc = aBuilder.build (m_aCryptoFactory.getCrypto ());

    // The attachment callback handler contains the encrypted attachments
    // Important: read the attachment stream only once!
    ICommonsList <WSS4JAttachment> aEncryptedAttachments = null;
    if (aAttachmentCallbackHandler != null)
    {
      aEncryptedAttachments = aAttachmentCallbackHandler.getAllResponseAttachments ();
      // MIME Type and CTE must be set for encrypted attachments!
      for (final WSS4JAttachment aAttachment : aEncryptedAttachments)
      {
        aAttachment.overwriteMimeType (CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
        aAttachment.setContentTransferEncoding (EContentTransferEncoding.BINARY);
      }
    }

    // Use the encrypted attachments!
    return MimeMessageCreator.generateMimeMessage (eSOAPVersion, aEncryptedDoc, aEncryptedAttachments);
  }
}
