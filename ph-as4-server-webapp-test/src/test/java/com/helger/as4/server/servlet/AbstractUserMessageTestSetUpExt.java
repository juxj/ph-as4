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
package com.helger.as4.server.servlet;

import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.AS4TestConstants;
import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractUserMessageTestSetUpExt extends AbstractUserMessageTestSetUp
{
  protected static final String DEFAULT_AGREEMENT = "urn:as4:agreements:so-that-we-have-a-non-empty-value";

  /**
   * Modify the standard user message to try special cases or provoke failure
   * messages.
   *
   * @param sAnotherOrWrongPModeID
   * @param sAnotherOrWrongPartyIdInitiator
   * @param sAnotherOrWrongPartyIdResponder
   * @param aEbms3MessageProperties
   *        Default should be with _defaultProperties(), only if you do not want
   *        them change this
   * @return
   * @throws Exception
   */
  @Nonnull
  protected Document _modifyUserMessage (@Nullable final String sAnotherOrWrongPModeID,
                                         @Nullable final String sAnotherOrWrongPartyIdInitiator,
                                         @Nullable final String sAnotherOrWrongPartyIdResponder,
                                         @Nullable final Ebms3MessageProperties aEbms3MessageProperties,
                                         @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                         @Nullable final String sReferenceToMessageID,
                                         @Nullable final Consumer <String> aMessagingIDConsumer) throws Exception
  {
    // If argument is set replace the default one
    final String sSetPartyIDInitiator = sAnotherOrWrongPartyIdInitiator == null ? DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdInitiator;
    final String sSetPartyIDResponder = sAnotherOrWrongPartyIdResponder == null ? DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdResponder;
    Ebms3PayloadInfo aEbms3PayloadInfo;
    Node aPayload = null;
    if (aAttachments == null)
    {
      aPayload = DOMReader.readXMLDOM (new ClassPathResource (AS4TestConstants.TEST_SOAP_BODY_PAYLOAD_XML));
      aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (aPayload != null, null);
    }
    else
    {
      aEbms3PayloadInfo = MessageHelperMethods.createEbms3PayloadInfo (false, aAttachments);
    }

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo (sReferenceToMessageID);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = MessageHelperMethods.createEbms3CollaborationInfo (sAnotherOrWrongPModeID,
                                                                                                              DEFAULT_AGREEMENT,
                                                                                                              AS4TestConstants.TEST_SERVICE_TYPE,
                                                                                                              AS4TestConstants.TEST_SERVICE,
                                                                                                              AS4TestConstants.TEST_ACTION,
                                                                                                              AS4TestConstants.TEST_CONVERSATION_ID);
    final Ebms3PartyInfo aEbms3PartyInfo = MessageHelperMethods.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                                                      sSetPartyIDInitiator,
                                                                                      CAS4.DEFAULT_RESPONDER_URL,
                                                                                      sSetPartyIDResponder);

    final AS4UserMessage aMsg = AS4UserMessage.create (aEbms3MessageInfo,
                                                       aEbms3PayloadInfo,
                                                       aEbms3CollaborationInfo,
                                                       aEbms3PartyInfo,
                                                       aEbms3MessageProperties,
                                                       ESOAPVersion.AS4_DEFAULT)
                                              .setMustUnderstand (true);

    if (aMessagingIDConsumer != null)
      aMessagingIDConsumer.accept (aMsg.getMessagingID ());

    return aAttachments != null ? aMsg.getAsSOAPDocument (null) : aMsg.getAsSOAPDocument (aPayload);
  }

  @Nonnull
  protected static Predicate <IPMode> _getFirstPModeWithID (@Nonnull final String sID)
  {
    return p -> p.getID ().equals (sID);
  }

  @Nonnull
  protected static Ebms3MessageProperties _defaultProperties ()
  {
    // Add properties
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (AS4TestConstants.getEBMSProperties ());
    return aEbms3MessageProperties;
  }
}
