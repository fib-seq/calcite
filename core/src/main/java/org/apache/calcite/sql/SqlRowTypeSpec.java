/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A sql type specification of row type, the grammar definition in SQL-2011 IWD 9075-2:201?(E)
 * 6.1 &lt;data type&gt; is as following:
 * <blockquote><pre>
 * &lt;row type&gt; ::=
 *   ROW &lt;row type body&gt;
 * &lt;row type body&gt; ::=
 *   &lt;left paren&gt; &lt;field definition&gt;
 *   [ { &lt;comma&gt; &lt;field definition&gt; }... ]
 *   &lt;right paren&gt;
 *
 * &lt;field definition&gt; ::=
 *   &lt;field name&gt; &lt;data type&gt;
 * </pre></blockquote>
 *
 * <p>We also support to add a [ NULL | NOT NULL ] suffix for every field type, i.e.
 * Row(f0 int null, f1 varchar not null), the default is not nullable.
 */
public class SqlRowTypeSpec extends SqlTypeNameSpec {

  private final List<SqlIdentifier> fieldNames;
  private final List<SqlDataTypeSpec> fieldTypes;

  /**
   * Creates a row type specification.
   *
   * @param pos        The parser position.
   * @param fieldNames The field names.
   * @param fieldTypes The field data types.
   */
  public SqlRowTypeSpec(
      SqlParserPos pos,
      List<SqlIdentifier> fieldNames,
      List<SqlDataTypeSpec> fieldTypes) {
    super(SqlTypeName.ROW.getName(), pos);
    Objects.requireNonNull(fieldNames);
    Objects.requireNonNull(fieldTypes);
    assert fieldNames.size() > 0; // there must be at least one field.
    this.fieldNames = fieldNames;
    this.fieldTypes = fieldTypes;
  }

  public List<SqlIdentifier> getFieldNames() {
    return fieldNames;
  }

  public List<SqlDataTypeSpec> getFieldTypes() {
    return fieldTypes;
  }

  public int getArity() {
    return fieldNames.size();
  }

  @Override public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.print(SqlTypeName.ROW.getName());
    SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.FUN_CALL, "(", ")");
    for (Pair<SqlIdentifier, SqlDataTypeSpec> p : Pair.zip(this.fieldNames, this.fieldTypes)) {
      writer.sep(",", false);
      p.left.unparse(writer, 0, 0);
      p.right.unparse(writer, leftPrec, rightPrec);
      if (p.right.getNullable() != null && p.right.getNullable()) {
        // Row fields default is not nullable.
        writer.print("NULL");
      }
    }
    writer.endList(frame);
  }

  @Override public RelDataType deriveType(RelDataTypeFactory typeFactory) {
    return typeFactory.createStructType(
        fieldTypes.stream()
            .map(dt -> dt.deriveType(typeFactory))
            .collect(Collectors.toList()),
        fieldNames.stream()
            .map(SqlIdentifier::toString)
            .collect(Collectors.toList()));
  }
}

// End SqlRowTypeSpec.java
