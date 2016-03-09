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

package com.google.domain.registry.xjc;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.domain.registry.util.ResourceUtils.readResourceUtf8;
import static com.google.domain.registry.xjc.XjcXmlTransformer.unmarshal;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.domain.registry.testing.ExceptionRule;
import com.google.domain.registry.xjc.rde.XjcRdeDeposit;
import com.google.domain.registry.xjc.rde.XjcRdeDepositTypeType;
import com.google.domain.registry.xjc.rdecontact.XjcRdeContact;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

/** Unit tests for {@code XjcObject}. */
@RunWith(JUnit4.class)
public class XjcObjectTest {

  @Rule
  public final ExceptionRule thrown = new ExceptionRule();

  private static final String RDE_DEPOSIT_FULL =
      readResourceUtf8(XjcObjectTest.class, "testdata/rde_deposit_full.xml");

  @Test
  public void testMarshalUtf8() throws Exception {
    XjcRdeDeposit deposit = unmarshalFullDeposit();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    deposit.marshal(out, UTF_8);
    String xml = out.toString(UTF_8.toString());
    Pattern pat = Pattern.compile("^<\\?xml version=\"1\\.0\" encoding=\"UTF[-_]?8\"");
    assertWithMessage("bad xml declaration: " + xml).that(pat.matcher(xml).find()).isTrue();
    assertWithMessage("encode/decode didn't work: " + xml).that(xml).contains("Jane Doe");
  }

  @Test
  public void testMarshalUtf16() throws Exception {
    XjcRdeDeposit deposit = unmarshalFullDeposit();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    deposit.marshal(out, UTF_16);
    String xml = out.toString(UTF_16.toString());
    Pattern pat = Pattern.compile("^<\\?xml version=\"1\\.0\" encoding=\"UTF[-_]?16\"");
    assertWithMessage(xml).that(pat.matcher(xml).find()).isTrue();
    assertWithMessage("encode/decode didn't work: " + xml).that(xml).contains("Jane Doe");
  }

  @Test
  public void testMarshalValidation() throws Exception {
    XjcRdeDeposit deposit = unmarshalFullDeposit();
    thrown.expect(Throwable.class, "pattern '\\w{1,13}' for type 'depositIdType'");
    deposit.setId("");
    deposit.marshal(new ByteArrayOutputStream(), UTF_8);
  }

  @Test
  public void testUnmarshalUTF8() throws Exception {
    XjcRdeDeposit deposit = unmarshalFullDeposit();
    assertThat(deposit).isNotNull();
    assertThat(deposit.getType()).isEqualTo(XjcRdeDepositTypeType.FULL);
    assertThat(deposit.getRdeMenu().getVersion()).isEqualTo("1.0");
  }

  @Test
  public void testUnmarshalUTF16() throws Exception {
    XjcRdeDeposit deposit = unmarshal(new ByteArrayInputStream(
        RDE_DEPOSIT_FULL.replaceFirst("UTF-8", "UTF-16").getBytes(UTF_16)));
    assertThat(deposit).isNotNull();
    assertThat(deposit.getType()).isEqualTo(XjcRdeDepositTypeType.FULL);
    assertThat(deposit.getRdeMenu().getVersion()).isEqualTo("1.0");
  }

  @Test
  public void testUnmarshalValidation() throws Exception {
    thrown.expect(Throwable.class, "pattern '\\w{1,13}' for type 'depositIdType'");
    unmarshal(new ByteArrayInputStream(
        RDE_DEPOSIT_FULL.replaceFirst("id=\"[^\"]+\"", "id=\"\"").getBytes(UTF_8)));
  }

  @Test
  public void testToString() throws Exception {
    String xml = unmarshalFullDeposit().toString();
    assertWithMessage(xml).that(xml).startsWith("<rde:deposit ");
    assertWithMessage(xml).that(xml.length()).isGreaterThan(1000);
  }

  @Test
  public void testToStringNoValidation() {
    String xml = new XjcRdeContact().toString();
    assertWithMessage(xml).that(xml).startsWith("<XjcRdeContact ");
  }

  @Test
  public void testNamespaceEpp() throws Exception {
    String xml = unmarshal(new ByteArrayInputStream(readResourceUtf8(
        XjcObjectTest.class, "testdata/greeting.xml").getBytes(UTF_8))).toString();
    assertWithMessage(xml).that(xml).startsWith("<epp:epp ");
    assertWithMessage(xml).that(xml).contains("\"urn:ietf:params:xml:ns:epp-1.0\"");
    assertWithMessage(xml).that(xml).contains("<epp:greeting>");
  }

  /** Unmarshals XML assuming UTF-8 encoding. */
  private static XjcRdeDeposit unmarshalFullDeposit() throws Exception {
    return unmarshal(new ByteArrayInputStream(RDE_DEPOSIT_FULL.getBytes(UTF_8)));
  }
}