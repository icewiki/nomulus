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

package com.google.domain.registry.model.transfer;

import static com.google.common.truth.Truth.assertThat;
import static com.google.domain.registry.testing.DatastoreHelper.createTld;
import static com.google.domain.registry.testing.DatastoreHelper.persistResource;
import static com.google.domain.registry.util.DateTimeUtils.END_OF_TIME;
import static org.joda.money.CurrencyUnit.USD;
import static org.joda.time.DateTimeZone.UTC;

import com.google.common.collect.ImmutableSet;
import com.google.domain.registry.model.billing.BillingEvent;
import com.google.domain.registry.model.billing.BillingEvent.Reason;
import com.google.domain.registry.model.domain.DomainResource;
import com.google.domain.registry.model.poll.PollMessage;
import com.google.domain.registry.model.reporting.HistoryEntry;
import com.google.domain.registry.model.transfer.TransferData.TransferServerApproveEntity;
import com.google.domain.registry.testing.AppEngineRule;
import com.google.domain.registry.testing.DatastoreHelper;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

import org.joda.money.Money;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TransferData}. */
@RunWith(JUnit4.class)
public class TransferDataTest {

  @Rule
  public final AppEngineRule appEngine = AppEngineRule.builder()
      .withDatastore()
      .build();

  protected final DateTime now = DateTime.now(UTC);

  HistoryEntry historyEntry;
  TransferData transferData;
  BillingEvent.OneTime transferBillingEvent;
  BillingEvent.OneTime nonTransferBillingEvent;
  BillingEvent.OneTime otherTransferBillingEvent;
  BillingEvent.Recurring recurringBillingEvent;

  @Before
  public void setUp() {
    createTld("tld");
    DomainResource domain = DatastoreHelper.persistActiveDomain("tat.tld");
    historyEntry = persistResource(new HistoryEntry.Builder().setParent(domain).build());
    transferBillingEvent = persistResource(makeBillingEvent());

    nonTransferBillingEvent = persistResource(
        makeBillingEvent().asBuilder().setReason(Reason.CREATE).build());

    otherTransferBillingEvent = persistResource(
        makeBillingEvent().asBuilder().setCost(Money.of(USD, 33)).build());

    recurringBillingEvent = persistResource(
        new BillingEvent.Recurring.Builder()
            .setReason(Reason.AUTO_RENEW)
            .setClientId("TheRegistrar")
            .setTargetId("foo.tld")
            .setEventTime(now)
            .setRecurrenceEndTime(END_OF_TIME)
            .setParent(historyEntry)
            .build());
  }

  private BillingEvent.OneTime makeBillingEvent() {
    return new BillingEvent.OneTime.Builder()
        .setReason(Reason.TRANSFER)
        .setClientId("TheRegistrar")
        .setTargetId("foo.tld")
        .setEventTime(now)
        .setBillingTime(now.plusDays(5))
        .setCost(Money.of(USD, 42))
        .setPeriodYears(3)
        .setParent(historyEntry)
        .build();
  }

  @SafeVarargs
  private static TransferData makeTransferDataWithEntities(
      Key<? extends TransferServerApproveEntity>... entityKeys) {
    ImmutableSet<Key<? extends TransferServerApproveEntity>> entityKeysSet =
        ImmutableSet.copyOf(entityKeys);
    return new TransferData.Builder().setServerApproveEntities(entityKeysSet).build();
  }

  @Test
  public void testSuccess_FindBillingEventNoEntities() throws Exception {
    transferData = makeTransferDataWithEntities();
    assertThat(transferData.serverApproveBillingEvent).isNull();
    assertThat(transferData.getServerApproveBillingEvent()).isNull();
  }

  @Test
  public void testSuccess_FindBillingEventOtherEntities() throws Exception {
    transferData = makeTransferDataWithEntities(
        Key.create(nonTransferBillingEvent),
        Key.create(recurringBillingEvent),
        Key.create(PollMessage.OneTime.class, 1));
    assertThat(transferData.serverApproveBillingEvent).isNull();
    assertThat(transferData.getServerApproveBillingEvent()).isNull();
  }

  @Test
  public void testSuccess_GetStoredBillingEventNoEntities() throws Exception {
    transferData = new TransferData.Builder()
        .setServerApproveBillingEvent(Ref.create(Key.create(transferBillingEvent)))
        .build();
    assertThat(transferData.serverApproveBillingEvent.get()).isEqualTo(transferBillingEvent);
    assertThat(transferData.getServerApproveBillingEvent().get()).isEqualTo(transferBillingEvent);
  }

  @Test
  public void testSuccess_GetStoredBillingEventMultipleEntities() throws Exception {
    transferData = makeTransferDataWithEntities(
        Key.create(otherTransferBillingEvent),
        Key.create(nonTransferBillingEvent),
        Key.create(recurringBillingEvent),
        Key.create(PollMessage.OneTime.class, 1));
    transferData = transferData.asBuilder()
        .setServerApproveBillingEvent(Ref.create(Key.create(transferBillingEvent)))
        .build();
    assertThat(transferData.serverApproveBillingEvent.get()).isEqualTo(transferBillingEvent);
    assertThat(transferData.getServerApproveBillingEvent().get()).isEqualTo(transferBillingEvent);
  }
}