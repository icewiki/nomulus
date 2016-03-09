// Copyright 2016 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.domain.registry.keyring.api;

import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Domain Registry keyring interface.
 *
 * <p>Separate methods are defined for each specific situation in which the
 * registry server needs a secret value, like a PGP key or password.
 */
@ThreadSafe
public interface Keyring extends AutoCloseable {

  /**
   * Returns the key which should be used to sign RDE deposits being uploaded to a third-party.
   *
   * <p>When we give all our data to the escrow provider, they'll need
   * a signature to ensure the data is authentic.
   *
   * <p>This keypair should only be known to the domain registry shared
   * registry system.
   *
   * @see com.google.domain.registry.rde.RdeUploadTask
   */
  PGPKeyPair getRdeSigningKey();

  /**
   * Returns public key for encrypting escrow deposits being staged to cloud storage.
   *
   * <p>This adds an additional layer of security so cloud storage administrators
   * won't be tempted to go poking around the App Engine Cloud Console and see a
   * dump of the entire database.
   *
   * <p>This keypair should only be known to the domain registry shared
   * registry system.
   *
   * @see #getRdeStagingDecryptionKey()
   */
  PGPPublicKey getRdeStagingEncryptionKey();

  /**
   * Returns private key for decrypting escrow deposits retrieved from cloud storage.
   *
   * <p>This method may impose restrictions on who can call it. For example, we'd want
   * to check that the caller isn't an HTTP request attacking a vulnerability in the
   * admin console. The request should originate from a backend task queue servlet
   * invocation of the RDE upload thing.
   *
   * @see #getRdeStagingEncryptionKey()
   * @see com.google.domain.registry.rde.RdeUploadTask
   */
  PGPPrivateKey getRdeStagingDecryptionKey();

  /**
   * Returns public key of escrow agent for encrypting deposits as they're uploaded.
   *
   * @see com.google.domain.registry.rde.RdeUploadTask
   */
  PGPPublicKey getRdeReceiverKey();

  /**
   * Returns the PGP key we use to sign Bulk Registration Data Access (BRDA) deposits.
   *
   * @see com.google.domain.registry.rde.BrdaCopyTask
   */
  PGPKeyPair getBrdaSigningKey();

  /**
   * Returns public key of receiver of Bulk Registration Data Access (BRDA) deposits.
   *
   * @see com.google.domain.registry.rde.BrdaCopyTask
   */
  PGPPublicKey getBrdaReceiverKey();

  /**
   * Returns public key for SSH client connections made by RDE.
   *
   * <p>This is a string containing what would otherwise be the contents of an
   * {@code ~/.ssh/id_rsa.pub} file. It's usually a single line with the name of
   * the algorithm, the base64 key, and the email address of the owner.
   *
   * @see com.google.domain.registry.rde.RdeUploadTask
   */
  String getRdeSshClientPublicKey();

  /**
   * Returns private key for SSH client connections made by RDE.
   *
   * <p>This is a string containing what would otherwise be the contents of an
   * {@code ~/.ssh/id_rsa} file. It's ASCII-armored text.
   *
   * <p>This method may impose restrictions on who can call it. For example, we'd want
   * to check that the caller isn't an HTTP request attacking a vulnerability in the
   * admin console. The request should originate from a backend task queue servlet
   * invocation of the RDE upload thing.
   *
   * @see com.google.domain.registry.rde.RdeUploadTask
   */
  String getRdeSshClientPrivateKey();

  /**
   * Returns password to be used when uploading reports to ICANN.
   *
   * @see com.google.domain.registry.rde.RdeReportTask
   */
  String getIcannReportingPassword();

  /**
   * Returns {@code user:password} login for TMCH MarksDB HTTP server DNL interface.
   *
   * @see com.google.domain.registry.tmch.TmchDnlTask
   */
  String getMarksdbDnlLogin();

  /**
   * Returns password for TMCH MarksDB HTTP server LORDN interface.
   *
   * @see "com.google.domain.registry.tmch.LordnRequestInitializer"
   */
  String getMarksdbLordnPassword();

  /**
   * Returns {@code user:password} login for TMCH MarksDB HTTP server SMDRL interface.
   *
   * @see com.google.domain.registry.tmch.TmchSmdrlTask
   */
  String getMarksdbSmdrlLogin();

  /**
   * Returns the credentials for a service account on the Google AppEngine project downloaded from
   * the Cloud Console dashboard in JSON format.
   */
  String getJsonCredential();

  /**
   * Returns Braintree API private key for Registry.
   *
   * <p>This is a base32 value copied from the Braintree website.
   *
   * @see com.google.domain.registry.config.ConfigModule#provideBraintreePublicKey
   */
  String getBraintreePrivateKey();

  // Don't throw so try-with-resources works better.
  @Override
  void close();
}