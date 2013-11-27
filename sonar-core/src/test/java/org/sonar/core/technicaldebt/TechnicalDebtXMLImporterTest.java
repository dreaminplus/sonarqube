/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.technicaldebt;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.api.technicaldebt.Requirement;
import org.sonar.api.technicaldebt.WorkUnit;
import org.sonar.api.utils.ValidationMessages;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class TechnicalDebtXMLImporterTest {

  @Test
  public void import_characteristics() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("import_characteristics.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(sqale.rootCharacteristics()).hasSize(2);
    assertThat(sqale.rootCharacteristics().get(0).key()).isEqualTo("PORTABILITY");
    assertThat(sqale.rootCharacteristics().get(1).key()).isEqualTo("MAINTAINABILITY");

    Characteristic portability = sqale.characteristicByKey("PORTABILITY");
    assertThat(portability.order()).isEqualTo(1);
    assertThat(portability.children()).hasSize(2);
    assertThat(portability.children().get(0).key()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(sqale.characteristicByKey("COMPILER_RELATED_PORTABILITY").parent().key()).isEqualTo("PORTABILITY");
    assertThat(portability.children().get(1).key()).isEqualTo("HARDWARE_RELATED_PORTABILITY");
    assertThat(sqale.characteristicByKey("HARDWARE_RELATED_PORTABILITY").parent().key()).isEqualTo("PORTABILITY");

    Characteristic maintainability = sqale.characteristicByKey("MAINTAINABILITY");
    assertThat(maintainability.order()).isEqualTo(2);
    assertThat(maintainability.children()).hasSize(1);
    assertThat(maintainability.children().get(0).key()).isEqualTo("READABILITY");
    assertThat(sqale.characteristicByKey("READABILITY").parent().key()).isEqualTo("MAINTAINABILITY");
  }

  @Test
  public void import_xml_with_linear_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_linear.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, messages);
  }

  @Test
  public void import_xml_with_linear_with_offset() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_linear_with_offset.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, WorkUnit.create(1.0, "h"), messages);
  }

  @Test
  public void convert_deprecated_linear_with_threshold_function_by_linear_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_deprecated_linear_with_threshold.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, WorkUnit.create(0.0, "h"), messages);
    assertThat(messages.getWarnings()).hasSize(1);
  }

  @Test
  public void ignore_deprecated_constant_per_file_function() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldImportXML_with_deprecated_constant_per_file.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getWarnings()).hasSize(1);

    // characteristics
    assertThat(sqale.rootCharacteristics()).hasSize(1);
    Characteristic efficiency = sqale.characteristicByKey("EFFICIENCY");
    assertThat(efficiency.requirements()).isEmpty();
  }

  @Test
  public void shouldBadlyFormattedImportXML() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();
    String xml = getFileContent("shouldImportXML_badly-formatted.xml");

    ValidationMessages messages = ValidationMessages.create();
    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    checkXmlCorrectlyImported(sqale, messages);
  }

  @Test
  public void shouldLogWarningIfRuleNotFound() {
    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();
    String xml = getFileContent("shouldLogWarningIfRuleNotFound.xml");
    ValidationMessages messages = ValidationMessages.create();

    TechnicalDebtModel sqale = new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getWarnings()).hasSize(1);
    assertThat(messages.getWarnings().get(0)).isEqualTo("Rule not found: [repository=findbugs, key=Foo]");

    // characteristics
    assertThat(sqale.rootCharacteristics()).hasSize(1);
    Characteristic efficiency = sqale.characteristicByKey("EFFICIENCY");
    assertThat(efficiency.requirements()).isEmpty();
    assertThat(messages.getWarnings().get(0)).contains("findbugs");
  }

  @Test
  public void shouldNotifyOnUnexpectedValueTypeInXml() throws Exception {

    TechnicalDebtRuleCache technicalDebtRuleCache = mockRuleCache();

    String xml = getFileContent("shouldRejectXML_with_invalid_value.xml");
    ValidationMessages messages = ValidationMessages.create();

    new TechnicalDebtXMLImporter().importXML(xml, messages, technicalDebtRuleCache);

    assertThat(messages.getErrors()).hasSize(1);
    assertThat(messages.getErrors().get(0)).isEqualTo("Cannot import value 'abc' for field factor - Expected a numeric value instead");
  }

  private TechnicalDebtRuleCache mockRuleCache() {
    RuleFinder finder = Mockito.mock(RuleFinder.class);
    Mockito.when(finder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Lists.newArrayList(Rule.create("checkstyle", "Regexp", "Regular expression")));
    return new TechnicalDebtRuleCache(finder);
  }

  private void checkXmlCorrectlyImported(TechnicalDebtModel sqale, ValidationMessages messages) {
    checkXmlCorrectlyImported(sqale, WorkUnit.create(), messages);
  }

  private void checkXmlCorrectlyImported(TechnicalDebtModel sqale, WorkUnit offset, ValidationMessages messages) {
    assertThat(messages.getErrors()).isEmpty();

    // characteristics
    assertThat(sqale.rootCharacteristics()).hasSize(2);
    Characteristic efficiency = sqale.characteristicByKey("EFFICIENCY");
    assertThat(efficiency.name()).isEqualTo("Efficiency");

    // sub-characteristics
    assertThat(efficiency.children()).hasSize(1);
    Characteristic memoryEfficiency = sqale.characteristicByKey("MEMORY_EFFICIENCY");
    assertThat(memoryEfficiency.name()).isEqualTo("Memory use");

    // requirement
    assertThat(memoryEfficiency.requirements()).hasSize(1);
    Requirement requirement = memoryEfficiency.requirements().get(0);
    assertThat(requirement.ruleKey().repository()).isEqualTo("checkstyle");
    assertThat(requirement.ruleKey().rule()).isEqualTo("Regexp");
    assertThat(requirement.function()).isEqualTo("linear");
    assertThat(requirement.factor()).isEqualTo(WorkUnit.create(3.2, "h"));
    assertThat(requirement.offset()).isEqualTo(offset);
  }

  private String getFileContent(String file) {
    try {
      return Resources.toString(Resources.getResource(TechnicalDebtXMLImporterTest.class, "TechnicalDebtXMLImporterTest/" + file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
