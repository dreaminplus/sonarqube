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

package org.sonar.server.group;

import org.sonar.api.ServerComponent;
import org.sonar.core.user.GroupMembershipQuery;
import org.sonar.server.util.RubyUtils;

import java.util.Map;

/**
 * Used by ruby code <pre>Internal.group_membership</pre>
 */
public class InternalGroupMembershipService implements ServerComponent {

  private final GroupMembershipFinder finder;

  public InternalGroupMembershipService(GroupMembershipFinder finder) {
    this.finder = finder;
  }

  public GroupMembershipQueryResult find(Map<String, Object> params) {
    return finder.find(parseQuery(params));
  }

  private GroupMembershipQuery parseQuery(Map<String, Object> params) {
    GroupMembershipQuery.Builder builder = GroupMembershipQuery.builder();
    builder.membership(membership(params));
    builder.groupSearch((String) params.get("query"));
    builder.pageIndex(RubyUtils.toInteger(params.get("page")));
    builder.pageSize(RubyUtils.toInteger(params.get("pageSize")));
    builder.login((String) params.get("user"));
    return builder.build();
  }

  private String membership(Map<String, Object> params) {
    String selected = (String) params.get("selected");
    if ("selected".equals(selected)) {
      return GroupMembershipQuery.IN;
    } else if ("deselected".equals(selected)) {
      return GroupMembershipQuery.OUT;
    } else {
      return GroupMembershipQuery.ANY;
    }
  }
}
