/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.security.object.ObjectHelper;
import com.helger.xml.microdom.IMicroDocument;

public class PModeConfigManager extends AbstractMapBasedWALDAO <IPModeConfig, PModeConfig>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PModeConfigManager.class);
  private static final String ATTR_DEFAULT_ID = "defaultpmode";

  private String m_sDefaultID = null;

  public PModeConfigManager (@Nullable final String sFilename) throws DAOException
  {
    super (PModeConfig.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    final EChange ret = super.onRead (aDoc);
    m_sDefaultID = aDoc.getDocumentElement ().getAttributeValue (ATTR_DEFAULT_ID);
    return ret;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument ret = super.createWriteData ();
    ret.getDocumentElement ().setAttribute (ATTR_DEFAULT_ID, m_sDefaultID);
    return ret;
  }

  @Nonnull
  public IPModeConfig createPModeConfig (@Nonnull final PModeConfig aPModeConfig)
  {
    ValueEnforcer.notNull (aPModeConfig, "PModeConfig");

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aPModeConfig);
    });
    AuditHelper.onAuditCreateSuccess (PModeConfig.OT, aPModeConfig.getID ());
    s_aLogger.info ("Created PModeConfig with ID '" + aPModeConfig.getID () + "'");

    return aPModeConfig;
  }

  @Nonnull
  public EChange updatePModeConfig (@Nonnull final IPModeConfig aPModeConfig)
  {
    ValueEnforcer.notNull (aPModeConfig, "PModeConfig");
    final PModeConfig aRealPModeConfig = getOfID (aPModeConfig.getID ());
    if (aRealPModeConfig == null)
    {
      AuditHelper.onAuditModifyFailure (PModeConfig.OT, aPModeConfig.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      ObjectHelper.setLastModificationNow (aRealPModeConfig);
      internalUpdateItem (aRealPModeConfig);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (PModeConfig.OT, "all", aRealPModeConfig.getID ());
    s_aLogger.info ("Updated PModeConfig with ID '" + aPModeConfig.getID () + "'");

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange markPModeConfigDeleted (@Nullable final String sPModeConfigID)
  {
    final PModeConfig aDeletedPModeConfig = getOfID (sPModeConfigID);
    if (aDeletedPModeConfig == null)
    {
      AuditHelper.onAuditDeleteFailure (PModeConfig.OT, "no-such-object-id", sPModeConfigID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (ObjectHelper.setDeletionNow (aDeletedPModeConfig).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (PModeConfig.OT, "already-deleted", sPModeConfigID);
        return EChange.UNCHANGED;
      }
      internalMarkItemDeleted (aDeletedPModeConfig);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PModeConfig.OT, sPModeConfigID);
    s_aLogger.info ("Marked PModeConfig with ID '" + aDeletedPModeConfig.getID () + "' as deleted");

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deletePModeConfig (@Nullable final String sPModeConfigID)
  {
    final PModeConfig aDeletedPModeConfig = getOfID (sPModeConfigID);
    if (aDeletedPModeConfig == null)
    {
      AuditHelper.onAuditDeleteFailure (PModeConfig.OT, "no-such-object-id", sPModeConfigID);
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalDeleteItem (sPModeConfigID);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (PModeConfig.OT, sPModeConfigID);

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IPModeConfig> getAllPModeConfigs ()
  {
    return getAll ();
  }

  @Nullable
  public IPModeConfig getPModeConfigOfID (@Nullable final String sID)
  {
    IPModeConfig ret = getOfID (sID);
    if (ret == null && m_sDefaultID != null)
    {
      // ID not found - try default
      ret = getOfID (m_sDefaultID);
    }
    return ret;
  }

  @Nullable
  public String getDefaultPModeConfigID ()
  {
    return m_sDefaultID;
  }

  public void setDefaultPModeConfigID (@Nullable final String sDefaultPModeConfigID)
  {
    m_sDefaultID = sDefaultPModeConfigID;
  }

  @Nonnull
  public ESuccess validatePModeConfig (@Nullable final IPModeConfig aPModeConfig)
  {

    if (aPModeConfig == null)
    {
      throw new IllegalStateException ("PModeConfig is null!");
    }

    // Needs ID
    if (aPModeConfig.getID () == null)
    {
      throw new IllegalStateException ("No PModeConfig ID present");
    }

    // MEPBINDING only push maybe push and pull
    if (aPModeConfig.getMEPBinding () == null)
    {
      throw new IllegalStateException ("No PModeConfig MEPBinding present. (Push, Pull, Sync)");
    }

    // Checking MEP all are allowed
    if (aPModeConfig.getMEP () == null)
    {
      throw new IllegalStateException ("No PModeConfig MEP present");
    }
    return ESuccess.SUCCESS;
  }

  public void validateAllPModeConfigs ()
  {
    for (final IPModeConfig aPModeConfig : getAll ())
      validatePModeConfig (aPModeConfig);
  }
}
